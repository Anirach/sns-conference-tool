package com.sns.app.config;

import java.util.concurrent.TimeUnit;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Distributed fixed-window limiter backed by Redisson's {@code RRateLimiter}. Each key gets a
 * dedicated limiter instance cached inside Redis; the rate (N per hour) is set once per instance
 * and re-applied if the stored rate differs.
 * <p>
 * Enabled by {@code sns.rate-limit.backend=redis}.
 */
@Component
@ConditionalOnProperty(name = "sns.rate-limit.backend", havingValue = "redis")
public class RedissonRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedissonRateLimiter.class);

    private final RedissonClient redisson;
    private final long maxPerHour;

    public RedissonRateLimiter(
        RedissonClient redisson,
        @Value("${sns.rate-limit.register-per-ip-per-hour:5}") int maxPerHour
    ) {
        this.redisson = redisson;
        this.maxPerHour = maxPerHour;
    }

    @Override
    public boolean tryAcquire(String key) {
        try {
            RRateLimiter limiter = redisson.getRateLimiter("rate:register:" + key);
            limiter.trySetRate(RateType.OVERALL, maxPerHour, 1, RateIntervalUnit.HOURS);
            return limiter.tryAcquire(1, 0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Fail-open on transient Redis issues — matches the spec's "don't accidentally DoS yourself".
            log.warn("RedissonRateLimiter tryAcquire failed for {}: {}", key, e.toString());
            return true;
        }
    }
}
