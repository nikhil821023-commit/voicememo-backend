package com.voicememo.repository;

import com.voicememo.model.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    List<ActionItem> findByMemoIdOrderByPriorityAsc(Long memoId);

    // All incomplete action items across all memos
    List<ActionItem> findByCompletedFalseOrderByCreatedAtDesc();

    // All action items for a memo, completed or not
    List<ActionItem> findByMemoId(Long memoId);

    // Only HIGH priority incomplete items
    List<ActionItem> findByPriorityAndCompletedFalse(ActionItem.Priority priority);
}