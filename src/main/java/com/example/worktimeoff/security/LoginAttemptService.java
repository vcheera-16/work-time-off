package com.example.worktimeoff.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory login attempt tracking. Not suitable for multi-node production.
 */
@Service
public class LoginAttemptService {
    private static class Attempts {
        List<Instant> attempts = new ArrayList<>();
    }

    private final Map<String, Attempts> store = new ConcurrentHashMap<>();
    private final int maxAttempts = 5;
    private final long windowSeconds = 15 * 60; // 15 minutes

    public boolean isBlocked(String key) {
        Attempts a = store.get(key);
        if (a == null) return false;
        prune(a);
        return a.attempts.size() >= maxAttempts;
    }

    public void recordFailure(String key) {
        Attempts a = store.computeIfAbsent(key, k -> new Attempts());
        prune(a);
        a.attempts.add(Instant.now());
    }

    public void reset(String key) {
        store.remove(key);
    }

    private void prune(Attempts a) {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        Iterator<Instant> it = a.attempts.iterator();
        while (it.hasNext()) {
            if (it.next().isBefore(cutoff)) it.remove();
        }
    }
}
