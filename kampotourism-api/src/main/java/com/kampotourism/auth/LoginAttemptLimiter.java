package com.kampotourism.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tiny in-memory brute-force guard for the one public write-ish endpoint (login).
 * Keyed by client IP + email so an attacker cannot lock the real admin out from elsewhere.
 * In-memory is fine for a single instance; use Redis or the proxy's rate limiting if you scale out.
 */
@Component
public class LoginAttemptLimiter {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record State(int failures, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, State> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        State state = attempts.get(key);
        return state != null && state.lockedUntil() != null && Instant.now().isBefore(state.lockedUntil());
    }

    public void recordFailure(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            Instant now = Instant.now();
            attempts.values().removeIf(s -> s.lockedUntil() != null && now.isAfter(s.lockedUntil()));
        }
        attempts.merge(key, new State(1, null), (old, ignored) -> {
            int failures = old.failures() + 1;
            return failures >= MAX_FAILURES ? new State(0, Instant.now().plus(LOCK)) : new State(failures, null);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }
}
