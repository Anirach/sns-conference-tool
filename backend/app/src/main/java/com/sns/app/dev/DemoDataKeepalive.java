package com.sns.app.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the seeded {@link DemoDataSeeder} dataset perpetually visible to the vicinity query.
 *
 * <p>{@code VicinityService} excludes any peer whose {@code participations.last_update} is older
 * than 5 minutes — sensible in production (a fellow who walked off the venue floor shouldn't
 * appear nearby), fatal for a static demo dataset whose timestamps go stale within minutes of
 * boot. Without this, the Fellows page goes empty 5 minutes after seeding.
 *
 * <p>The job runs once a minute, refreshes only rows that are about to expire (so it's a no-op
 * cost when everything is fresh), and clears the {@code vicinity} cache so the next request
 * doesn't serve a 10-second-old empty result. Gated on the same dev flag as the seeder.
 */
@Component
@ConditionalOnProperty(name = "sns.dev.seed-demo-data", havingValue = "true")
public class DemoDataKeepalive {

    private static final Logger log = LoggerFactory.getLogger(DemoDataKeepalive.class);

    private final JdbcTemplate jdbc;
    private final CacheManager cacheManager;

    public DemoDataKeepalive(JdbcTemplate jdbc, CacheManager cacheManager) {
        this.jdbc = jdbc;
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void bumpStaleParticipations() {
        int updated = jdbc.update(
            "UPDATE participations SET last_update = now() "
            + "WHERE last_update < now() - INTERVAL '4 minutes'"
        );
        if (updated > 0) {
            var cache = cacheManager.getCache("vicinity");
            if (cache != null) cache.clear();
            log.debug("DemoDataKeepalive: bumped {} stale participations and cleared vicinity cache", updated);
        }
    }
}
