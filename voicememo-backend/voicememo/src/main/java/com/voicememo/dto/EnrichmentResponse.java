package com.voicememo.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrichmentResponse {

    private Long memoId;
    private String summary;
    private List<String> suggestedTags;
    private List<ActionItemResponse> actionItems;
    private Integer wordCount;
    private Integer readingTimeSeconds;
    private Integer sentenceCount;
}