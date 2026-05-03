package com.voicememo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    // FREE or PRO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

    // Stripe customer ID for billing portal
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    // Stripe subscription ID for cancellation
    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    // When PRO expires (null = active)
    @Column(name = "pro_expires_at")
    private LocalDateTime proExpiresAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Memo> memos;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isPro() {
        return plan == Plan.PRO &&
                (proExpiresAt == null || LocalDateTime.now().isBefore(proExpiresAt));
    }

    public enum Plan { FREE, PRO }
}