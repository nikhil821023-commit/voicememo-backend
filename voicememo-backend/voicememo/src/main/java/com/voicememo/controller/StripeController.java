package com.voicememo.controller;

import com.voicememo.model.User;
import com.voicememo.repository.UserRepository;
import com.voicememo.service.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StripeController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    // POST /api/v1/billing/checkout
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> checkout() throws Exception {
        User user = getCurrentUser();
        String url = stripeService.createCheckoutSession(user);
        return ResponseEntity.ok(Map.of("checkoutUrl", url));
    }

    // POST /api/v1/billing/portal
    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> portal() throws Exception {
        User user = getCurrentUser();
        String url = stripeService.createBillingPortalSession(user);
        return ResponseEntity.ok(Map.of("portalUrl", url));
    }

    // POST /api/v1/billing/webhook — Stripe calls this (no auth needed)
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sig) throws Exception {
        stripeService.handleWebhook(payload, sig);
        return ResponseEntity.ok().build();
    }

    // ─── reads email from JWT via SecurityContext ─────────────────────────────
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Not authenticated");
        }
        String email = auth.getPrincipal().toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}