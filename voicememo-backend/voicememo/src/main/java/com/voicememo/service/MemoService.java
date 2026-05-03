package com.voicememo.service;

import com.voicememo.dto.MemoResponse;
import com.voicememo.dto.PagedResponse;
import com.voicememo.model.Memo;
import com.voicememo.model.User;
import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final TranscriptionService transcriptionService;
    private final AiEnrichmentService aiEnrichmentService;
    private final UserRepository userRepository;

    @Value("${app.audio.storage-path}")
    private String storagePath;

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD + TRANSCRIBE — fixed: attaches current user to memo
    // ─────────────────────────────────────────────────────────────────────────
    public MemoResponse uploadAndTranscribe(MultipartFile audioFile,
                                            String title,
                                            Integer durationSeconds, String language) throws IOException {

        // Step 1 — Get current logged-in user
        User currentUser = getCurrentUser();

        // Step 2 — Save audio file to disk
        Path savedAudioPath = saveAudioFile(audioFile);
        log.info("Audio saved: {}", savedAudioPath);

        // Step 3 — Auto-generate title if not provided
        String memoTitle = (title != null && !title.isBlank())
                ? title
                : "Memo - " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));

        // Step 4 — Create DB record with PENDING status + attach user
        Memo memo = Memo.builder()
                .title(memoTitle)
                .audioFilePath(savedAudioPath.toString())
                .audioFilename(audioFile.getOriginalFilename())
                .durationSeconds(durationSeconds)
                .status(Memo.MemoStatus.PENDING)
                .user(currentUser)          // ← FIXED: was missing before
                .build();

        memo = memoRepository.save(memo);
        log.info("Memo created with ID: {} for user: {}", memo.getId(), currentUser.getEmail());

        // Step 5 — Transcribe
        try {
            memo.setStatus(Memo.MemoStatus.TRANSCRIBING);
            memoRepository.save(memo);

            // Call transcribeWithLanguage which returns both at once
            TranscriptionService.TranscriptionResult result =
                    transcriptionService.transcribeWithLanguage(savedAudioPath, language);


            memo.setTranscription(result.text());
            memo.setDetectedLanguage(result.language());
            memo.setStatus(Memo.MemoStatus.DONE);

        } catch (Exception e) {
            log.error("Transcription failed for memo {}: {}", memo.getId(), e.getMessage(), e);
            memo.setStatus(Memo.MemoStatus.FAILED);
        }

        // Step 6 — Save final state
        memo = memoRepository.save(memo);

        // Step 7 — Run AI enrichment async after DONE
        if (memo.getStatus() == Memo.MemoStatus.DONE) {
            final Long memoId = memo.getId();
            CompletableFuture.runAsync(() -> aiEnrichmentService.enrichMemo(memoId));
        }

        return MemoResponse.from(memo);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL — scoped to current user
    // ─────────────────────────────────────────────────────────────────────────
    public PagedResponse<MemoResponse> getAllMemos(int page, int size, String sortBy) {
        Long userId = getCurrentUserId();

        // Safety check — never proceed without a real user ID
        if (userId == null) {
            throw new RuntimeException("Not authenticated");
        }

        Pageable pageable = buildPageable(page, size, sortBy);
        Page<MemoResponse> result = memoRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(MemoResponse::from);
        return PagedResponse.from(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ONE — security check: user can only see their own memo
    // ─────────────────────────────────────────────────────────────────────────
    public MemoResponse getMemoById(Long id) {
        Long userId = getCurrentUserId();
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + id));

        if (memo.getUser() == null || !memo.getUser().getId().equals(userId)) {
            throw new RuntimeException("Memo not found: " + id);
        }

        return MemoResponse.from(memo);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE — security check: user can only delete their own memo
    // ─────────────────────────────────────────────────────────────────────────
    public void deleteMemo(Long id) {
        Long userId = getCurrentUserId();
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + id));

        if (memo.getUser() == null || !memo.getUser().getId().equals(userId)) {
            throw new RuntimeException("Memo not found: " + id);
        }

        try {
            Files.deleteIfExists(Path.of(memo.getAudioFilePath()));
            log.info("Audio file deleted: {}", memo.getAudioFilePath());
        } catch (IOException e) {
            log.warn("Could not delete audio file: {}", e.getMessage());
        }

        memoRepository.deleteById(id);
        log.info("Memo deleted: {}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENAME — security check: user can only rename their own memo
    // ─────────────────────────────────────────────────────────────────────────
    public MemoResponse updateTitle(Long id, String newTitle) {
        Long userId = getCurrentUserId();
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + id));

        if (memo.getUser() == null || !memo.getUser().getId().equals(userId)) {
            throw new RuntimeException("Memo not found: " + id);
        }

        memo.setTitle(newTitle);
        return MemoResponse.from(memoRepository.save(memo));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH — scoped to current user
    // ─────────────────────────────────────────────────────────────────────────
    public PagedResponse<MemoResponse> searchMemos(String query,
                                                   Long folderId,
                                                   int page, int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<MemoResponse> result;

        if (folderId != null) {
            result = memoRepository
                    .searchByUserInFolder(userId, query, folderId, pageable)
                    .map(MemoResponse::from);
        } else {
            result = memoRepository
                    .searchByUser(userId, query, pageable)
                    .map(MemoResponse::from);
        }
        return PagedResponse.from(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BY FOLDER — scoped to current user
    // ─────────────────────────────────────────────────────────────────────────
    public PagedResponse<MemoResponse> getMemosByFolder(Long folderId,
                                                        int page, int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<MemoResponse> result = memoRepository
                .findByUserIdAndFolderIdOrderByCreatedAtDesc(userId, folderId, pageable)
                .map(MemoResponse::from);
        return PagedResponse.from(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BY TAG — scoped to current user
    // ─────────────────────────────────────────────────────────────────────────
    public PagedResponse<MemoResponse> getMemosByTag(String tagName,
                                                     int page, int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<MemoResponse> result = memoRepository
                .findByUserIdAndTagName(userId, tagName, pageable)
                .map(MemoResponse::from);
        return PagedResponse.from(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BY DATE RANGE — scoped to current user
    // ─────────────────────────────────────────────────────────────────────────
    public PagedResponse<MemoResponse> getMemosByDateRange(LocalDate from,
                                                           LocalDate to,
                                                           int page, int size) {
        Long userId = getCurrentUserId();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);
        Pageable pageable = PageRequest.of(page, size);
        Page<MemoResponse> result = memoRepository
                .findByUserIdAndDateRange(userId, fromDt, toDt, pageable)
                .map(MemoResponse::from);
        return PagedResponse.from(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Returns full User object (needed for attaching to memo)
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        String email = auth.getPrincipal().toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // Returns just the user ID (used for filtering queries)
    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private Path saveAudioFile(MultipartFile file) throws IOException {
        Path storageDir = Path.of(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        String uniqueName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path destination  = storageDir.resolve(uniqueName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private Pageable buildPageable(int page, int size, String sortBy) {
        return switch (sortBy != null ? sortBy : "date") {
            case "title"    -> PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "title"));
            case "duration" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "durationSeconds"));
            default         -> PageRequest.of(page, size);
        };
    }

}