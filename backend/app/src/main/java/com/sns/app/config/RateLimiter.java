package com.sns.app.config;

/**
 * Abstraction over the rate-limit backend. Two implementations ship:
 * <ul>
 *   <li>{@link InMemoryRateLimiter} (default, {@code sns.rate-limit.backend=memory}) —
 *       fixed-window {@code ConcurrentHashMap}. Fine for dev/tests and single-pod.</li>
 *   <li>{@link RedissonRateLimiter} ({@code sns.rate-limit.backend=redis}) — Redisson
 *       {@code RRateLimiter}. Use in prod with multiple pods.</li>
 * </ul>
 */
public interface RateLimiter {
    /** @return true if the request is allowed, false if it should be rejected. */
    boolean tryAcquire(String key);
}
