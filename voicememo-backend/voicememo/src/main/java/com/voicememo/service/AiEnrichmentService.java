package com.voicememo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicememo.model.ActionItem;
import com.voicememo.model.Memo;
import com.voicememo.model.Tag;
import com.voicememo.repository.ActionItemRepository;
import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEnrichmentService {

    private final MemoRepository memoRepository;
    private final ActionItemRepository actionItemRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.openai.api-key}")
    private String openAiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini"; // fast + cheap for enrichment

    // ─── Main entry point — called after every successful transcription ───────

    @Transactional
    public void enrichMemo(Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        if (memo.getTranscription() == null || memo.getTranscription().isBlank()) {
            log.warn("Memo {} has no transcription to enrich", memoId);
            return;
        }

        log.info("Starting AI enrichment for memo {}", memoId);

        try {
            // Step 1 — Compute text stats (no AI needed, instant)
            applyTextStats(memo);

            // Step 2 — AI: summarize + auto-tag + extract actions in ONE call
            String jsonResult = callOpenAiEnrichment(memo.getTranscription());

            // Step 3 — Parse and save each enrichment
            JsonNode root = objectMapper.readTree(jsonResult);

            applySummary(memo, root);
            applyAutoTags(memo, root);
            applyActionItems(memo, root);

            memo.setEnriched(true);
            memoRepository.save(memo);

            log.info("Enrichment complete for memo {}", memoId);

        } catch (Exception e) {
            log.error("Enrichment failed for memo {}: {}", memoId, e.getMessage());
            // Don't fail the memo — enrichment is best-effort
        }
    }

    // ─── Step 1: Text stats (pure Java, no API call needed) ──────────────────

    private void applyTextStats(Memo memo) {
        String text = memo.getTranscription().trim();

        // Word count
        String[] words = text.split("\\s+");
        int wordCount = words.length;

        // Sentence count (split on . ! ?)
        String[] sentences = text.split("[.!?]+");
        int sentenceCount = (int) Arrays.stream(sentences)
                .filter(s -> !s.isBlank())
                .count();

        // Reading time: average 200 words/minute for spoken transcription
        int readingTimeSecs = (int) Math.ceil((wordCount / 200.0) * 60);

        memo.setWordCount(wordCount);
        memo.setSentenceCount(sentenceCount);
        memo.setReadingTimeSeconds(readingTimeSecs);

        log.info("Stats — words: {}, sentences: {}, readTime: {}s",
                wordCount, sentenceCount, readingTimeSecs);
    }

    // ─── Step 2: Single OpenAI call for all 3 AI enrichments ─────────────────

    private String callOpenAiEnrichment(String transcription) {
        String prompt = """
            You are an AI assistant that enriches voice memo transcriptions.
            
            Given this voice memo transcription, return ONLY a valid JSON object
            with exactly these three keys — no markdown, no explanation:
            
            {
              "summary": "A 1-2 sentence plain-English summary of the memo.",
              "tags": ["tag1", "tag2", "tag3"],
              "action_items": [
                {
                  "text": "What needs to be done",
                  "priority": "HIGH|MEDIUM|LOW",
                  "deadline_hint": "by Friday (or null if none mentioned)"
                }
              ]
            }
            
            Rules:
            - summary: concise, no fluff, max 2 sentences
            - tags: 1-5 lowercase single-word or hyphenated tags (e.g. "work", "follow-up")
            - action_items: only real tasks/todos. Empty array if none found.
            - priority: HIGH if urgent/today, MEDIUM if this week, LOW otherwise
            - deadline_hint: null if no deadline mentioned
            
            Transcription:
            """ + transcription;

        WebClient client = WebClient.builder()
                .baseUrl(OPENAI_URL)
                .defaultHeader("Authorization", "Bearer " + openAiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 800
        );

        Map response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // Extract content from OpenAI response structure
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }

    // ─── Step 3a: Apply summary ───────────────────────────────────────────────

    private void applySummary(Memo memo, JsonNode root) {
        if (root.has("summary") && !root.get("summary").isNull()) {
            String summary = root.get("summary").asText().trim();
            memo.setSummary(summary);
            log.info("Summary set: {}", summary.substring(0, Math.min(60, summary.length())));
        }
    }

    // ─── Step 3b: Apply auto-tags ─────────────────────────────────────────────

    private void applyAutoTags(Memo memo, JsonNode root) {
        if (!root.has("tags") || root.get("tags").isNull()) return;

        JsonNode tagsNode = root.get("tags");
        tagsNode.forEach(tagNode -> {
            String tagName = tagNode.asText().toLowerCase().trim();
            if (tagName.isBlank()) return;

            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(
                            Tag.builder().name(tagName).build()));

            if (memo.getTags() == null) {
                memo.setTags(new ArrayList<>());
            }

            boolean alreadyTagged = memo.getTags().stream()
                    .anyMatch(t -> t.getName().equals(tagName));

            if (!alreadyTagged) {
                memo.getTags().add(tag);
                log.info("Auto-tag added: {}", tagName);
            }
        });
    }

    // ─── Step 3c: Apply action items ─────────────────────────────────────────

    private void applyActionItems(Memo memo, JsonNode root) {
        if (!root.has("action_items") || root.get("action_items").isNull()) return;

        // Remove old AI-generated action items before replacing
        actionItemRepository.deleteAll(
                actionItemRepository.findByMemoId(memo.getId()));

        JsonNode items = root.get("action_items");
        items.forEach(itemNode -> {
            String text = itemNode.has("text") ? itemNode.get("text").asText().trim() : "";
            if (text.isBlank()) return;

            String priorityStr = itemNode.has("priority")
                    ? itemNode.get("priority").asText().toUpperCase()
                    : "MEDIUM";

            ActionItem.Priority priority;
            try {
                priority = ActionItem.Priority.valueOf(priorityStr);
            } catch (IllegalArgumentException e) {
                priority = ActionItem.Priority.MEDIUM;
            }

            String deadlineHint = itemNode.has("deadline_hint") &&
                    !itemNode.get("deadline_hint").isNull()
                    ? itemNode.get("deadline_hint").asText()
                    : null;

            ActionItem action = ActionItem.builder()
                    .memo(memo)
                    .text(text)
                    .priority(priority)
                    .deadlineHint(deadlineHint)
                    .completed(false)
                    .build();

            actionItemRepository.save(action);
            log.info("Action item saved [{}]: {}", priority, text);
        });
    }

    // ─── Re-enrich on demand (called from controller) ─────────────────────────

    @Transactional
    public void reEnrich(Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));
        memo.setEnriched(false);
        memoRepository.save(memo);
        enrichMemo(memoId);
    }
}