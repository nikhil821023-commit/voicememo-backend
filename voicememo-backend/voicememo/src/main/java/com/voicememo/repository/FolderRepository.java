package com.voicememo.repository;

import com.voicememo.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    // ← FIXED: scope all queries to user
    List<Folder> findByUserIdOrderByNameAsc(Long userId);

    Optional<Folder> findByNameAndUserId(String name, Long userId);

    boolean existsByNameAndUserId(String name, Long userId);
}