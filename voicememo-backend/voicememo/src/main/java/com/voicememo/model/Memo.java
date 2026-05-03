package com.voicememo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "memos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User-given title (or auto-generated)
    @Column(nullable = false)
    private String title;

    // Path to the saved audio file on disk
    @Column(name = "audio_file_path")
    private String audioFilePath;

    // Original audio filename
    @Column(name = "audio_filename")
    private String audioFilename;

    // The full transcription text from Whisper
    @Column(columnDefinition = "TEXT")
    private String transcription;

    // Duration in seconds
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Status: PENDING, TRANSCRIBING, DONE, FAILED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoStatus status;

    // Language detected by Whisper
    @Column(name = "detected_language")
    private String detectedLanguage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Inside Memo class — ADD these fields below the existing ones

    // Which folder this memo lives in (nullable = no folder)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    // Tags (many-to-many)
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "memo_tags",
            joinColumns = @JoinColumn(name = "memo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    // ADD inside Memo class — below the Pass 2 fields

    // 1-2 sentence AI-generated summary
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Word count of the transcription
    @Column(name = "word_count")
    private Integer wordCount;

    // Estimated reading time in seconds
    @Column(name = "reading_time_seconds")
    private Integer readingTimeSeconds;

    // Total sentence count
    @Column(name = "sentence_count")
    private Integer sentenceCount;

    // Whether AI enrichment has been run
    @Column(name = "enriched")
    private boolean enriched = false;

    // ADD to existing Memo class

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Action items extracted from this memo
    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActionItem> actionItems = new ArrayList<>();

    public enum MemoStatus {
        PENDING,        // Audio uploaded, not yet transcribed
        TRANSCRIBING,   // Currently being processed
        DONE,           // Transcription complete
        FAILED          // Something went wrong
    }
}