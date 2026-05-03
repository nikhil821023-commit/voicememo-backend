package com.voicememo.dto;

import com.voicememo.model.Folder;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {

    private Long id;
    private String name;
    private String description;
    private int memoCount;
    private LocalDateTime createdAt;

    public static FolderResponse from(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .memoCount(folder.getMemos() != null ? folder.getMemos().size() : 0)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}