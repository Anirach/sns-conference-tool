package com.sns.app.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the seeded {@link DemoDataSeeder} dataset perpetually live in dev.
 *
 * <p>Two things would otherwise age out and break the demo:
 * <ul>
 *   <li>{@code participations.last_update} — {@code VicinityService} excludes peers whose
 *       fix is older than 5 minutes, sensible in prod (a fellow who left shouldn't appear
 *       nearby), fatal for a static dataset.</li>
 *   <li>{@code events.expiration_code} — {@code EventService.join} returns 400 "Event expired"
 *       once the seeded NeurIPS / ACL events pass their adjournment date. The seeder sets
 *       this to {@code now + 3 / 5 days} at seed time, so a dev DB that's been running for
 *       a week refuses every join.</li>
 * </ul>
 *
 * <p>The job runs once a minute, no-ops when nothing's stale, and clears the {@code vicinity}
 * cache after a participation bump so the next read sees fresh data. Gated on the same dev
 * flag as the seeder. ICML2025 is left alone — it's intentionally seeded as expired ("Adjourned").
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
    public void bumpStaleData() {
        int parts = jdbc.update(
            "UPDATE participations SET last_update = now() "
            + "WHERE last_update < now() - INTERVAL '4 minutes'"
        );
        if (parts > 0) {
            var cache = cacheManager.getCache("vicinity");
            if (cache != null) cache.clear();
            log.debug("DemoDataKeepalive: bumped {} stale participations + cleared vicinity cache", parts);
        }

        // Push the expiration of seeded active events forward whenever they fall under a week
        // of headroom. ICML2025 stays expired (it's the "Adjourned" demo case).
        int events = jdbc.update(
            "UPDATE events SET expiration_code = now() + INTERVAL '30 days' "
            + "WHERE qr_code_plaintext IN ('NEURIPS2026', 'ACL2026') "
            + "  AND expiration_code < now() + INTERVAL '7 days'"
        );
        if (events > 0) {
            log.debug("DemoDataKeepalive: extended {} demo event expirations to now+30d", events);
        }
    }
}
