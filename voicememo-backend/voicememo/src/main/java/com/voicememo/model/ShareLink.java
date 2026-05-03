package com.voicememo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The memo this share link points to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    // Random UUID token used in the URL
    @Column(nullable = false, unique = true)
    private String token;

    // When this link expires (null = never expires)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Whether the owner has manually revoked it
    @Column(nullable = false)
    private boolean active = true;

    // How many times this link has been accessed
    @Column(name = "access_count", nullable = false)
    private int accessCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return active && !isExpired();
    }
}