package com.voicememo.controller;

import com.voicememo.dto.ActionItemResponse;
import com.voicememo.service.ActionItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ActionItemController {

    private final ActionItemService actionItemService;

    // GET /api/v1/actions — all pending action items across all memos
    @GetMapping
    public ResponseEntity<List<ActionItemResponse>> getAllPending() {
        return ResponseEntity.ok(actionItemService.getAllPending());
    }

    // GET /api/v1/actions/memo/{memoId} — action items for a specific memo
    @GetMapping("/memo/{memoId}")
    public ResponseEntity<List<ActionItemResponse>> getByMemo(@PathVariable Long memoId) {
        return ResponseEntity.ok(actionItemService.getByMemo(memoId));
    }

    // GET /api/v1/actions/priority/HIGH — filter by priority
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<ActionItemResponse>> getByPriority(
            @PathVariable String priority) {
        return ResponseEntity.ok(actionItemService.getByPriority(priority));
    }

    // PATCH /api/v1/actions/{id}/complete — mark done
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ActionItemResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(actionItemService.markComplete(id));
    }

    // PATCH /api/v1/actions/{id}/incomplete — unmark done
    @PatchMapping("/{id}/incomplete")
    public ResponseEntity<ActionItemResponse> incomplete(@PathVariable Long id) {
        return ResponseEntity.ok(actionItemService.markIncomplete(id));
    }

    // DELETE /api/v1/actions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        actionItemService.deleteActionItem(id);
        return ResponseEntity.noContent().build();
    }
}