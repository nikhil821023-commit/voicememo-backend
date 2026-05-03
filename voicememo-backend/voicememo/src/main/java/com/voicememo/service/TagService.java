package com.voicememo.service;

import com.voicememo.model.Memo;
import com.voicememo.model.Tag;
import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final MemoRepository memoRepository;

    // Get or create a tag by name
    public Tag getOrCreate(String name) {
        return tagRepository.findByName(name.toLowerCase().trim())
                .orElseGet(() -> tagRepository.save(
                        Tag.builder().name(name.toLowerCase().trim()).build()));
    }

    @Transactional
    public List<String> addTagsToMemo(Long memoId, List<String> tagNames) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        tagNames.forEach(name -> {
            Tag tag = getOrCreate(name);
            if (!memo.getTags().contains(tag)) {
                memo.getTags().add(tag);
            }
        });

        memoRepository.save(memo);
        return memo.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    }

    @Transactional
    public List<String> removeTagFromMemo(Long memoId, String tagName) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + memoId));

        memo.getTags().removeIf(t -> t.getName().equals(tagName.toLowerCase().trim()));
        memoRepository.save(memo);

        return memo.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    }

    public List<String> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(Tag::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}