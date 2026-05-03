package com.voicememo.service;

import com.voicememo.dto.ActionItemResponse;
import com.voicememo.model.ActionItem;
import com.voicememo.repository.ActionItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionItemService {

    private final ActionItemRepository actionItemRepository;

    public List<ActionItemResponse> getByMemo(Long memoId) {
        return actionItemRepository.findByMemoIdOrderByPriorityAsc(memoId)
                .stream()
                .map(ActionItemResponse::from)
                .collect(Collectors.toList());
    }

    public List<ActionItemResponse> getAllPending() {
        return actionItemRepository.findByCompletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(ActionItemResponse::from)
                .collect(Collectors.toList());
    }

    public List<ActionItemResponse> getByPriority(String priority) {
        ActionItem.Priority p = ActionItem.Priority.valueOf(priority.toUpperCase());
        return actionItemRepository.findByPriorityAndCompletedFalse(p)
                .stream()
                .map(ActionItemResponse::from)
                .collect(Collectors.toList());
    }

    public ActionItemResponse markComplete(Long id) {
        ActionItem item = actionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Action item not found: " + id));
        item.setCompleted(true);
        return ActionItemResponse.from(actionItemRepository.save(item));
    }

    public ActionItemResponse markIncomplete(Long id) {
        ActionItem item = actionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Action item not found: " + id));
        item.setCompleted(false);
        return ActionItemResponse.from(actionItemRepository.save(item));
    }

    public void deleteActionItem(Long id) {
        actionItemRepository.deleteById(id);
    }
}