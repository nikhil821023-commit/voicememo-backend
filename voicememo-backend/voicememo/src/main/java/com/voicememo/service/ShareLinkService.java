package com.voicememo.service;

import com.voicememo.dto.MemoResponse;
import com.voicememo.dto.ShareLinkResponse;
import com.voicememo.model.Memo;
import com.voicememo.model.ShareLink;
import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final MemoRepository memoRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // Create a new share link for a memo
    public ShareLinkResponse createShareLink(Long memoId, Integer expiryHours) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        String token = UUID.randomUUID().toString().replace("-", "");

        LocalDateTime expiresAt = expiryHours != null
                ? LocalDateTime.now().plusHours(expiryHours)
                : null;

        ShareLink link = ShareLink.builder()
                .memo(memo)
                .token(token)
                .expiresAt(expiresAt)
                .active(true)
                .accessCount(0)
                .build();

        link = shareLinkRepository.save(link);
        log.info("Share link created for memo {}: {}", memoId, token);
        return ShareLinkResponse.from(link, baseUrl);
    }

    // Resolve a share token → return the memo (read-only)
    @Transactional
    public MemoResponse resolveShareLink(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Share link not found"));

        if (!link.isValid()) {
            throw new RuntimeException(
                    link.isExpired() ? "Share link has expired" : "Share link is no longer active"
            );
        }

        // Increment access counter
        link.setAccessCount(link.getAccessCount() + 1);
        shareLinkRepository.save(link);

        return MemoResponse.from(link.getMemo());
    }

    // Get all share links for a memo
    public List<ShareLinkResponse> getLinksForMemo(Long memoId) {
        return shareLinkRepository.findByMemoIdAndActiveTrue(memoId)
                .stream()
                .map(l -> ShareLinkResponse.from(l, baseUrl))
                .collect(Collectors.toList());
    }

    // Revoke (deactivate) a share link
    public void revokeLink(Long linkId) {
        ShareLink link = shareLinkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Share link not found: " + linkId));
        link.setActive(false);
        shareLinkRepository.save(link);
        log.info("Share link {} revoked", linkId);
    }

    // Revoke ALL share links for a memo
    public void revokeAllForMemo(Long memoId) {
        shareLinkRepository.findByMemoId(memoId)
                .forEach(l -> {
                    l.setActive(false);
                    shareLinkRepository.save(l);
                });
        log.info("All share links revoked for memo {}", memoId);
    }
}