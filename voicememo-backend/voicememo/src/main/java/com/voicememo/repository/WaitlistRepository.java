package com.voicememo.repository;

import com.voicememo.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    boolean existsByEmail(String email);
    long countBySource(String source);
}