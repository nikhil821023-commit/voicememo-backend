package com.voicememo.controller;

import com.voicememo.dto.FolderRequest;
import com.voicememo.dto.FolderResponse;
import com.voicememo.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FolderController {

    private final FolderService folderService;

    // POST /api/v1/folders — create folder
    @PostMapping
    public ResponseEntity<FolderResponse> create(@Valid @RequestBody FolderRequest request) {
        return ResponseEntity.ok(folderService.createFolder(request));
    }

    // GET /api/v1/folders — list all folders
    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAll() {
        return ResponseEntity.ok(folderService.getAllFolders());
    }

    // GET /api/v1/folders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(folderService.getFolderById(id));
    }

    // PATCH /api/v1/folders/{id}/rename
    @PatchMapping("/{id}/rename")
    public ResponseEntity<FolderResponse> rename(@PathVariable Long id,
                                                 @RequestParam String newName) {
        return ResponseEntity.ok(folderService.renameFolder(id, newName));
    }

    // DELETE /api/v1/folders/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        folderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/folders/move?memoId=1&folderId=2
    @PatchMapping("/move")
    public ResponseEntity<Void> moveMemo(@RequestParam Long memoId,
                                         @RequestParam(required = false) Long folderId) {
        folderService.moveMemoToFolder(memoId, folderId);
        return ResponseEntity.ok().build();
    }
}