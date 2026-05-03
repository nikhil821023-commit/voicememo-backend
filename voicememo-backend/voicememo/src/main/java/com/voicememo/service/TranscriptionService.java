package com.voicememo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final WebClient whisperWebClient;

    @Value("${app.whisper.model}")
    private String whisperModel;

    @Value("${app.whisper.language:}")
    private String defaultLanguage;

    public record TranscriptionResult(String text, String language) {}

    // ── Main method — accepts optional language hint ───────────────
    public TranscriptionResult transcribeWithLanguage(Path audioFilePath, String language) {
        log.info("Sending audio to Groq Whisper: {}", audioFilePath.getFileName());

        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(audioFilePath));
            bodyBuilder.part("model", whisperModel);
            bodyBuilder.part("response_format", "verbose_json");

            // Prefer method param → fallback to config default → fallback to auto
            String langToUse = (language != null && !language.isBlank())
                    ? language
                    : (defaultLanguage != null && !defaultLanguage.isBlank())
                    ? defaultLanguage
                    : null;

            if (langToUse != null) {
                bodyBuilder.part("language", langToUse);
                log.info("Using language hint: {}", langToUse);
            } else {
                log.info("Auto-detecting language");
            }

            Map response = whisperWebClient.post()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Empty response from Whisper");
            }

            String text     = response.containsKey("text")
                    ? (String) response.get("text") : "";
            String detected = response.containsKey("language")
                    ? (String) response.get("language") : "unknown";

            log.info("Transcription done. Language: {}, Chars: {}", detected, text.length());
            return new TranscriptionResult(text, detected);

        } catch (Exception e) {
            log.error("Transcription failed: {}", e.getMessage());
            throw new RuntimeException("Transcription failed: " + e.getMessage());
        }
    }

    // ── Overload — no language hint (uses default from config) ─────
    public TranscriptionResult transcribeWithLanguage(Path audioFilePath) {
        return transcribeWithLanguage(audioFilePath, null);
    }

    // ── Backward-compatible methods ────────────────────────────────
    public String transcribe(Path audioFilePath) {
        return transcribeWithLanguage(audioFilePath, null).text();
    }

    public String detectLanguage(Path audioFilePath) {
        return transcribeWithLanguage(audioFilePath, null).language();
    }
}