package com.voicememo.controller;

import com.voicememo.dto.MemoResponse;
import com.voicememo.dto.PagedResponse;
import com.voicememo.service.AiEnrichmentService;
import com.voicememo.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/memos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow mobile app to connect
public class MemoController {

    private final MemoService memoService;
    private final AiEnrichmentService aiEnrichmentService;

    /**
     * POST /api/v1/memos/upload
     * Upload audio file → transcribe → save → return memo
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemoResponse> uploadMemo(
            @RequestPart("audio") MultipartFile audioFile,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
                    @RequestParam(value = "language", required = false) String language
    ) {
        log.info("Received audio upload: {}, size: {} bytes",
                audioFile.getOriginalFilename(), audioFile.getSize());

        try {
            MemoResponse response = memoService.uploadAndTranscribe(audioFile, title, durationSeconds, language);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/v1/memos?page=0&size=20&sort=date
     * Get all memos (paginated)
     */
    @GetMapping
    public ResponseEntity<PagedResponse<MemoResponse>> getAllMemos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sort
    ) {
        return ResponseEntity.ok(memoService.getAllMemos(page, size, sort));
    }

    /**
     * GET /api/v1/memos/{id}
     * Get single memo by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MemoResponse> getMemo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(memoService.getMemoById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/v1/memos/{id}/title
     * Rename a memo
     */
    @PatchMapping("/{id}/title")
    public ResponseEntity<MemoResponse> updateTitle(
            @PathVariable Long id,
            @RequestParam String newTitle
    ) {
        try {
            return ResponseEntity.ok(memoService.updateTitle(id, newTitle));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/v1/memos/{id}
     * Delete memo + audio file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemo(@PathVariable Long id) {
        try {
            memoService.deleteMemo(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/memos/search?q=meeting&folderId=1&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<MemoResponse>> search(
            @RequestParam String q,
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(memoService.searchMemos(q, folderId, page, size));
    }

    /**
     * GET /api/v1/memos/by-folder/{folderId}?page=0&size=20
     */
    @GetMapping("/by-folder/{folderId}")
    public ResponseEntity<PagedResponse<MemoResponse>> getByFolder(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(memoService.getMemosByFolder(folderId, page, size));
    }

    /**
     * GET /api/v1/memos/by-tag?tag=work&page=0&size=20
     */
    @GetMapping("/by-tag")
    public ResponseEntity<PagedResponse<MemoResponse>> getByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(memoService.getMemosByTag(tag, page, size));
    }

    /**
     * GET /api/v1/memos/by-date?from=2026-01-01&to=2026-04-30&page=0&size=20
     */
    @GetMapping("/by-date")
    public ResponseEntity<PagedResponse<MemoResponse>> getByDate(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                memoService.getMemosByDateRange(LocalDate.parse(from), LocalDate.parse(to), page, size)
        );
    }
    // POST /api/v1/memos/{id}/enrich — manually trigger re-enrichment
    @PostMapping("/{id}/enrich")
    public ResponseEntity<Void> reEnrich(@PathVariable Long id) {
        CompletableFuture.runAsync(() -> aiEnrichmentService.reEnrich(id));
        return ResponseEntity.accepted().build(); // 202 — processing async
    }

    // Add this private method to any controller that needs the logged-in user
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        return auth.getPrincipal().toString();
    }
}