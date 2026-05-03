package com.voicememo.controller;

import com.voicememo.repository.TagRepository;
import com.voicememo.repository.UserRepository;
import com.voicememo.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TagController {

    private final TagService tagService;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    // ← FIXED: only return tags for current user's memos
    @GetMapping
    public ResponseEntity<List<String>> getAllTags() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal().toString();
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        List<String> tags = tagRepository.findTagsByUserId(userId)
                .stream()
                .map(t -> t.getName())
                .collect(Collectors.toList());

        return ResponseEntity.ok(tags);
    }

    @PostMapping("/memo/{memoId}")
    public ResponseEntity<List<String>> addTags(@PathVariable Long memoId,
                                                @RequestBody List<String> tags) {
        return ResponseEntity.ok(tagService.addTagsToMemo(memoId, tags));
    }

    @DeleteMapping("/memo/{memoId}")
    public ResponseEntity<List<String>> removeTag(@PathVariable Long memoId,
                                                  @RequestParam String tag) {
        return ResponseEntity.ok(tagService.removeTagFromMemo(memoId, tag));
    }
}