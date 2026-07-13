package com.joao.storemanagement.security;

import com.joao.storemanagement.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String username) {
        AttemptState state = attempts.get(username);
        if (state == null || state.lockedUntil == null) {
            return;
        }
        if (state.lockedUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException("登录失败次数过多，请稍后再试");
        }
        attempts.remove(username);
    }

    public void recordFailure(String username) {
        AttemptState state = attempts.compute(username, (key, current) -> {
            AttemptState next = current == null ? new AttemptState() : current;
            next.failedCount++;
            if (next.failedCount >= MAX_ATTEMPTS) {
                next.lockedUntil = LocalDateTime.now().plus(LOCK_DURATION);
            }
            return next;
        });
        if (state.lockedUntil != null && state.lockedUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException("登录失败次数过多，请稍后再试");
        }
    }

    public void reset(String username) {
        attempts.remove(username);
    }

    private static class AttemptState {
        private int failedCount;
        private LocalDateTime lockedUntil;
    }
}
