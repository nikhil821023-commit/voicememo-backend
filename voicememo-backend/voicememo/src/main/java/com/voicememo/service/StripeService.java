package com.voicememo.service;

import com.stripe.Stripe;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.voicememo.model.User;
import com.voicememo.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final UserRepository userRepository;

    @Value("${app.stripe.secret-key}")
    private String secretKey;

    @Value("${app.stripe.pro-price-id}")
    private String proPriceId;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    // Create Stripe Checkout session → user pays → webhook upgrades plan
    public String createCheckoutSession(User user) throws Exception {
        // Create or retrieve Stripe customer
        String customerId = ensureStripeCustomer(user);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(proPriceId)
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(baseUrl + "/upgrade/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/upgrade/cancel")
                .build();

        Session session = Session.create(params);
        log.info("Checkout session created for user {}", user.getEmail());
        return session.getUrl();
    }

    // Stripe calls this webhook when payment succeeds
    public void handleWebhook(String payload, String sigHeader) throws Exception {
        String webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                upgradeUserToPro(session.getCustomer(), session.getSubscription());
            }
            case "customer.subscription.deleted" -> {
                Subscription sub = (Subscription) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                downgradeUserToFree(sub.getCustomer());
            }
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    // Open Stripe billing portal for user to manage/cancel
    public String createBillingPortalSession(User user) throws Exception {
        String customerId = ensureStripeCustomer(user);

        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(
                        com.stripe.param.billingportal.SessionCreateParams.builder()
                                .setCustomer(customerId)
                                .setReturnUrl(baseUrl + "/account")
                                .build()
                );

        return session.getUrl();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String ensureStripeCustomer(User user) throws Exception {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("User email is required for billing");
        }

        Customer customer = Customer.create(
                CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(user.getName())
                        .putMetadata("userId", String.valueOf(user.getId()))
                        .build());

        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    private void upgradeUserToPro(String customerId, String subscriptionId) {
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setPlan(User.Plan.PRO);
            user.setStripeSubscriptionId(subscriptionId);
            user.setProExpiresAt(null); // Active subscription = no expiry
            userRepository.save(user);
            log.info("User {} upgraded to PRO", user.getEmail());
        });
    }

    private void downgradeUserToFree(String customerId) {
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setPlan(User.Plan.FREE);
            user.setStripeSubscriptionId(null);
            userRepository.save(user);
            log.info("User {} downgraded to FREE", user.getEmail());
        });
    }
}