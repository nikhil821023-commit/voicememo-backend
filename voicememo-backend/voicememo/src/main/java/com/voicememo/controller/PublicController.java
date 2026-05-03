package com.voicememo.controller;

import com.voicememo.model.WaitlistEntry;
import com.voicememo.repository.WaitlistRepository;
import com.voicememo.service.PublicStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

    private final PublicStatsService statsService;
    private final WaitlistRepository waitlistRepository;

    // GET /api/v1/stats — for landing page counters
    @GetMapping("/api/v1/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    // GET /api/v1/pricing — tier config for landing page
    @GetMapping("/api/v1/pricing")
    public ResponseEntity<List<Map<String, Object>>> pricing() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "name", "Free",
                        "price", 0,
                        "currency", "USD",
                        "features", List.of(
                                "10 voice memos",
                                "Basic transcription",
                                "Search and folders"
                        ),
                        "cta", "Get started"
                ),
                Map.of(
                        "name", "Pro",
                        "price", 4.99,
                        "currency", "USD",
                        "features", List.of(
                                "Unlimited memos",
                                "AI summarization",
                                "Auto-tagging",
                                "Action item extraction",
                                "PDF + TXT export",
                                "Public share links"
                        ),
                        "cta", "Upgrade to Pro"
                )
        ));
    }

    // POST /api/v1/waitlist — capture pre-launch emails
    @PostMapping("/api/v1/waitlist")
    public ResponseEntity<Map<String, String>> joinWaitlist(
            @RequestBody Map<String, String> body) {

        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String source = body.getOrDefault("source", "direct");

        if (email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Valid email required"));
        }

        if (waitlistRepository.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("message", "Already on the waitlist!"));
        }

        waitlistRepository.save(WaitlistEntry.builder()
                .email(email)
                .source(source)
                .build());

        return ResponseEntity.ok(Map.of("message", "You're on the list!"));
    }

    // GET /api/v1/health — for Railway / Docker health checks
    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "version", "1.0.0"));
    }
}