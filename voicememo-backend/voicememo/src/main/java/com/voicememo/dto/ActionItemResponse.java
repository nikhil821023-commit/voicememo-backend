package com.voicememo.dto;

import com.voicememo.model.ActionItem;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItemResponse {

    private Long id;
    private Long memoId;
    private String memoTitle;
    private String text;
    private String priority;
    private String deadlineHint;
    private boolean completed;
    private LocalDateTime createdAt;

    public static ActionItemResponse from(ActionItem item) {
        return ActionItemResponse.builder()
                .id(item.getId())
                .memoId(item.getMemo().getId())
                .memoTitle(item.getMemo().getTitle())
                .text(item.getText())
                .priority(item.getPriority() != null ? item.getPriority().name() : "MEDIUM")
                .deadlineHint(item.getDeadlineHint())
                .completed(item.isCompleted())
                .createdAt(item.getCreatedAt())
                .build();
    }
}