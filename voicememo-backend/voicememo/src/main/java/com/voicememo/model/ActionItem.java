package com.voicememo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "action_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The parent memo this action came from
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    // The action text e.g. "Call John about the proposal"
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    // HIGH / MEDIUM / LOW — extracted by AI
    @Enumerated(EnumType.STRING)
    private Priority priority;

    // Optional deadline extracted by AI e.g. "by Friday"
    @Column(name = "deadline_hint")
    private String deadlineHint;

    // Whether the user has marked this done
    @Column(nullable = false)
    private boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}