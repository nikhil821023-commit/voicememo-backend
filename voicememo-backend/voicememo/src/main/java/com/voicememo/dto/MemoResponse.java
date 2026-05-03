package com.voicememo.dto;

import com.voicememo.model.Memo;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoResponse {

    private Long id;
    private String title;
    private String transcription;
    private String status;
    private String detectedLanguage;
    private Integer durationSeconds;
    private String audioFilename;

    // Pass 2 additions
    private Long folderId;
    private String folderName;
    private List<String> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ADD these fields to your existing MemoResponse class

    private String summary;
    private Integer wordCount;
    private Integer readingTimeSeconds;
    private Integer sentenceCount;
    private boolean enriched;
    private List<ActionItemResponse> actionItems;

    public static MemoResponse from(Memo memo) {
        return MemoResponse.builder()
                .id(memo.getId())
                .title(memo.getTitle())
                .transcription(memo.getTranscription())
                .status(memo.getStatus().name())
                .detectedLanguage(memo.getDetectedLanguage())
                .durationSeconds(memo.getDurationSeconds())
                .audioFilename(memo.getAudioFilename())
                .folderId(memo.getFolder() != null ? memo.getFolder().getId() : null)
                .folderName(memo.getFolder() != null ? memo.getFolder().getName() : null)
                .tags(memo.getTags() != null
                        ? memo.getTags().stream().map(t -> t.getName()).collect(Collectors.toList())
                        : List.of())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .summary(memo.getSummary())
                .wordCount(memo.getWordCount())
                .readingTimeSeconds(memo.getReadingTimeSeconds())
                .sentenceCount(memo.getSentenceCount())
                .enriched(memo.isEnriched())
                .actionItems(memo.getActionItems() != null
                        ? memo.getActionItems().stream()
                        .map(ActionItemResponse::from)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}