package com.sns.app.config;

import java.util.concurrent.TimeUnit;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Distributed fixed-window limiter backed by Redisson's {@code RRateLimiter}. Each (bucket, key)
 * pair gets a dedicated limiter instance keyed at {@code rate:{bucket}:{key}}; {@code trySetRate}
 * is idempotent so repeated calls with the same budget are safe.
 * <p>
 * Enabled by {@code sns.rate-limit.backend=redis}.
 */
@Component
@ConditionalOnProperty(name = "sns.rate-limit.backend", havingValue = "redis")
public class RedissonRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedissonRateLimiter.class);

    private final RedissonClient redisson;

    public RedissonRateLimiter(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public boolean tryAcquire(String bucket, String key, int maxPerHour) {
        try {
            RRateLimiter limiter = redisson.getRateLimiter("rate:" + bucket + ":" + key);
            limiter.trySetRate(RateType.OVERALL, maxPerHour, 1, RateIntervalUnit.HOURS);
            return limiter.tryAcquire(1, 0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Fail-open on transient Redis issues — matches the spec's "don't accidentally DoS yourself".
            log.warn("RedissonRateLimiter tryAcquire failed for {}/{}: {}", bucket, key, e.toString());
            return true;
        }
    }
}
