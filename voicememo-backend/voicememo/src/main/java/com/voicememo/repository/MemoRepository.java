package com.voicememo.repository;

import com.voicememo.model.Memo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {

    // ── Scoped to user — get all memos for a user ─────────────────
    Page<Memo> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // ── Count memos for a user (used for FREE plan limit check) ───
    long countByUserId(Long userId);

    // ── Get all memos in a folder (used by FolderService) ─────────
    List<Memo> findByFolderIdOrderByCreatedAtDesc(Long folderId);

    // ── Get memos in a specific folder scoped to user ──────────────
    Page<Memo> findByUserIdAndFolderIdOrderByCreatedAtDesc(
            Long userId, Long folderId, Pageable pageable);

    // ── Get memos with no folder scoped to user ────────────────────
    Page<Memo> findByUserIdAndFolderIsNullOrderByCreatedAtDesc(
            Long userId, Pageable pageable);

    // ── Full-text search scoped to user ───────────────────────────
    @Query("SELECT m FROM Memo m WHERE m.user.id = :userId AND (" +
            "LOWER(m.transcription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Memo> searchByUser(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable);

    // ── Search within a folder scoped to user ─────────────────────
    @Query("SELECT m FROM Memo m WHERE m.user.id = :userId " +
            "AND m.folder.id = :folderId AND (" +
            "LOWER(m.transcription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Memo> searchByUserInFolder(
            @Param("userId") Long userId,
            @Param("query") String query,
            @Param("folderId") Long folderId,
            Pageable pageable);

    // ── Filter by tag scoped to user ──────────────────────────────
    @Query("SELECT m FROM Memo m JOIN m.tags t " +
            "WHERE m.user.id = :userId AND t.name = :tagName " +
            "ORDER BY m.createdAt DESC")
    Page<Memo> findByUserIdAndTagName(
            @Param("userId") Long userId,
            @Param("tagName") String tagName,
            Pageable pageable);

    // ── Filter by date range scoped to user ───────────────────────
    @Query("SELECT m FROM Memo m WHERE m.user.id = :userId " +
            "AND m.createdAt BETWEEN :from AND :to " +
            "ORDER BY m.createdAt DESC")
    Page<Memo> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}