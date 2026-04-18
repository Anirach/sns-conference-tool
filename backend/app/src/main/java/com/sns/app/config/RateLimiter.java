package com.sns.app.config;

/**
 * Abstraction over the rate-limit backend. Two implementations ship:
 * <ul>
 *   <li>{@link InMemoryRateLimiter} (default, {@code sns.rate-limit.backend=memory}) —
 *       fixed-window {@code ConcurrentHashMap}. Fine for dev/tests and single-pod.</li>
 *   <li>{@link RedissonRateLimiter} ({@code sns.rate-limit.backend=redis}) — Redisson
 *       {@code RRateLimiter}. Use in prod with multiple pods.</li>
 * </ul>
 *
 * <p>{@code bucket} groups limiters with different budgets (e.g. {@code "login_ip"} vs
 * {@code "login_email"}); {@code key} narrows within the bucket (the IP / email / etc.);
 * {@code maxPerHour} is the budget applied to the bucket. Backends treat the budget as
 * idempotent — first writer wins per (bucket,key) pair so repeated calls with the same budget
 * are safe.
 */
public interface RateLimiter {
    /** @return true if the request is allowed, false if it should be rejected. */
    boolean tryAcquire(String bucket, String key, int maxPerHour);
}
