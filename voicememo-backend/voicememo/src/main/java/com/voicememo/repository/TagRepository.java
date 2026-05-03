package com.voicememo.repository;

import com.voicememo.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    // ← NEW: get only tags used by a specific user's memos
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.memos m WHERE m.user.id = :userId ORDER BY t.name")
    List<Tag> findTagsByUserId(@Param("userId") Long userId);
}