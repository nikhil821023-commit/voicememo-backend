package com.voicememo.service;

import com.voicememo.model.User;
import com.voicememo.repository.MemoRepository;
import com.voicememo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanGateService {

    private static final int FREE_MEMO_LIMIT = 10;

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    // Call before creating a new memo
    public void checkMemoLimit(User user) {
        if (user.isPro()) return; // PRO = unlimited

        long count = memoRepository.countByUserId(user.getId());
        if (count >= FREE_MEMO_LIMIT) {
            throw new PlanLimitException(
                    "Free plan allows " + FREE_MEMO_LIMIT + " memos. " +
                            "Upgrade to PRO for unlimited memos.");
        }
    }

    // Call before any AI enrichment / export
    public void requirePro(User user, String feature) {
        if (!user.isPro()) {
            throw new PlanLimitException(
                    feature + " requires a PRO subscription. " +
                            "Upgrade at /upgrade");
        }
    }

    public static class PlanLimitException extends RuntimeException {
        public PlanLimitException(String msg) { super(msg); }
    }
}