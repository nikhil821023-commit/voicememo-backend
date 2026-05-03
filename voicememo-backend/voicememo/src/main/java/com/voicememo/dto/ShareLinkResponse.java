package com.voicememo.dto;

import com.voicememo.model.ShareLink;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLinkResponse {

    private Long id;
    private Long memoId;
    private String memoTitle;
    private String token;
    private String shareUrl;        // Full URL e.g. https://yourapp.com/share/abc123
    private LocalDateTime expiresAt;
    private boolean active;
    private int accessCount;
    private LocalDateTime createdAt;

    public static ShareLinkResponse from(ShareLink link, String baseUrl) {
        return ShareLinkResponse.builder()
                .id(link.getId())
                .memoId(link.getMemo().getId())
                .memoTitle(link.getMemo().getTitle())
                .token(link.getToken())
                .shareUrl(baseUrl + "/api/v1/share/" + link.getToken())
                .expiresAt(link.getExpiresAt())
                .active(link.isActive())
                .accessCount(link.getAccessCount())
                .createdAt(link.getCreatedAt())
                .build();
    }
}