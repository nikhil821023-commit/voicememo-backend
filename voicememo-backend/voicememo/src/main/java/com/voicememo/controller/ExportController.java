package com.voicememo.controller;

import com.voicememo.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/memos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExportController {

    private final ExportService exportService;

    // GET /api/v1/memos/{id}/export/txt
    @GetMapping("/{id}/export/txt")
    public ResponseEntity<byte[]> exportTxt(@PathVariable Long id) {
        byte[] content = exportService.exportAsTxt(id);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"memo-" + id + ".txt\"")
                .body(content);
    }

    // GET /api/v1/memos/{id}/export/pdf
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) throws IOException {
        byte[] content = exportService.exportAsPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"memo-" + id + ".pdf\"")
                .body(content);
    }
}