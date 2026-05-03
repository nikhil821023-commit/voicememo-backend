package com.voicememo.repository;

import com.voicememo.model.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findByMemoId(Long memoId);

    List<ShareLink> findByMemoIdAndActiveTrue(Long memoId);
}