package com.sns.app.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Process-local fixed-window limiter. Each (bucket, key) gets its own sliding hour window.
 * Enabled by default or when {@code sns.rate-limit.backend=memory}.
 */
@Component
@ConditionalOnProperty(name = "sns.rate-limit.backend", havingValue = "memory", matchIfMissing = true)
@ConditionalOnMissingBean(RateLimiter.class)
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String bucket, String key, int maxPerHour) {
        String composite = bucket + ":" + key;
        Window w = windows.compute(composite, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.start.isBefore(now.minus(Duration.ofHours(1)))) {
                return new Window(now, 1);
            }
            return new Window(existing.start, existing.count + 1);
        });
        return w.count <= maxPerHour;
    }

    private record Window(Instant start, int count) {}
}
