package com.voicememo.controller;

import com.voicememo.dto.MemoResponse;
import com.voicememo.dto.ShareLinkResponse;
import com.voicememo.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    // POST /api/v1/memos/{memoId}/share?expiryHours=24
    @PostMapping("/api/v1/memos/{memoId}/share")
    public ResponseEntity<ShareLinkResponse> createLink(
            @PathVariable Long memoId,
            @RequestParam(required = false) Integer expiryHours) {
        return ResponseEntity.ok(
                shareLinkService.createShareLink(memoId, expiryHours));
    }

    // GET /api/v1/memos/{memoId}/share — list all active links for a memo
    @GetMapping("/api/v1/memos/{memoId}/share")
    public ResponseEntity<List<ShareLinkResponse>> getLinks(@PathVariable Long memoId) {
        return ResponseEntity.ok(shareLinkService.getLinksForMemo(memoId));
    }

    // DELETE /api/v1/share/links/{linkId} — revoke one link
    @DeleteMapping("/api/v1/share/links/{linkId}")
    public ResponseEntity<Void> revokeLink(@PathVariable Long linkId) {
        shareLinkService.revokeLink(linkId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/v1/memos/{memoId}/share — revoke all links for a memo
    @DeleteMapping("/api/v1/memos/{memoId}/share")
    public ResponseEntity<Void> revokeAll(@PathVariable Long memoId) {
        shareLinkService.revokeAllForMemo(memoId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/share/{token} — public endpoint, no auth needed
    @GetMapping("/api/v1/share/{token}")
    public ResponseEntity<MemoResponse> resolveShare(@PathVariable String token) {
        return ResponseEntity.ok(shareLinkService.resolveShareLink(token));
    }
}