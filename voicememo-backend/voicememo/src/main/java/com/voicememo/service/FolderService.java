package com.voicememo.service;

import com.voicememo.dto.FolderRequest;
import com.voicememo.dto.FolderResponse;
import com.voicememo.model.Folder;
import com.voicememo.model.Memo;
import com.voicememo.model.User;
import com.voicememo.repository.FolderRepository;
import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final MemoRepository   memoRepository;
    private final UserRepository   userRepository;

    // ── Get current logged-in user ────────────────────────────────
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("Not authenticated");
        String email = auth.getPrincipal().toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Create folder — scoped to current user ────────────────────
    public FolderResponse createFolder(FolderRequest request) {
        User user = getCurrentUser();

        if (folderRepository.existsByNameAndUserId(request.getName(), user.getId())) {
            throw new RuntimeException("Folder already exists: " + request.getName());
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(user)              // ← attach user
                .build();

        folder = folderRepository.save(folder);
        log.info("Folder created: {} for user: {}", folder.getName(), user.getEmail());
        return FolderResponse.from(folder);
    }

    // ── Get all folders — only current user's ─────────────────────
    public List<FolderResponse> getAllFolders() {
        Long userId = getCurrentUser().getId();
        return folderRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList());
    }

    // ── Get one folder — verify ownership ─────────────────────────
    public FolderResponse getFolderById(Long id) {
        Long userId = getCurrentUser().getId();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found: " + id));
        if (!folder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Folder not found: " + id);
        }
        return FolderResponse.from(folder);
    }

    // ── Rename folder — verify ownership ──────────────────────────
    public FolderResponse renameFolder(Long id, String newName) {
        User user = getCurrentUser();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found: " + id));

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Folder not found: " + id);
        }
        if (folderRepository.existsByNameAndUserId(newName, user.getId())) {
            throw new RuntimeException("Folder name already taken: " + newName);
        }

        folder.setName(newName);
        return FolderResponse.from(folderRepository.save(folder));
    }

    // ── Delete folder — verify ownership + unlink memos ───────────
    @Transactional
    public void deleteFolder(Long id) {
        Long userId = getCurrentUser().getId();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found: " + id));

        if (!folder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Folder not found: " + id);
        }

        // Unlink all memos from this folder before deleting
        List<Memo> memos = memoRepository.findByFolderIdOrderByCreatedAtDesc(id);
        memos.forEach(m -> m.setFolder(null));
        memoRepository.saveAll(memos);

        folderRepository.delete(folder);
        log.info("Folder deleted: {}", folder.getName());
    }

    // ── Move memo to folder — verify both belong to user ──────────
    @Transactional
    public void moveMemoToFolder(Long memoId, Long folderId) {
        Long userId = getCurrentUser().getId();
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        if (!memo.getUser().getId().equals(userId)) {
            throw new RuntimeException("Memo not found: " + memoId);
        }

        if (folderId == null) {
            memo.setFolder(null);
        } else {
            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found: " + folderId));

            // Make sure folder belongs to same user
            if (!folder.getUser().getId().equals(userId)) {
                throw new RuntimeException("Folder not found: " + folderId);
            }
            memo.setFolder(folder);
        }

        memoRepository.save(memo);
        log.info("Memo {} moved to folder {}", memoId, folderId);
    }
}