package com.voicememo.service;

import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicStatsService {

    private final UserRepository userRepository;
    private final MemoRepository memoRepository;

    // Returns public numbers shown on landing page
    public Map<String, Object> getStats() {
        long totalUsers = userRepository.count();
        long totalMemos = memoRepository.count();

        // Approximate minutes of audio transcribed
        // (average 3 min per memo)
        long minutesTranscribed = totalMemos * 3;

        return Map.of(
                "totalUsers", totalUsers,
                "totalMemos", totalMemos,
                "minutesTranscribed", minutesTranscribed,
                "freePlanLimit", 10,
                "proPriceUsd", 4.99
        );
    }
}