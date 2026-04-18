package com.sns.app.gdpr;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes {@code audit_log} rows older than the configured retention window. Runs nightly per
 * {@code sns.audit.prune-cron}.
 *
 * <p>The trigger installed by {@code V9__audit_log_immutability.sql} blocks any UPDATE/DELETE
 * on {@code audit_log} unless the session GUC {@code app.audit_prune} is set. We set it inside
 * the same transaction before issuing the DELETE, so accidental deletes from anywhere else in
 * the app remain blocked.
 *
 * <p>Pages 500 rows at a time so a multi-million-row {@code audit_log} sweep doesn't lock the
 * table for minutes.
 */
@Component
public class AuditLogPruneJob {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPruneJob.class);
    private static final int PAGE_SIZE = 500;

    @PersistenceContext
    private EntityManager em;

    private final int retentionDays;

    public AuditLogPruneJob(@Value("${sns.audit.retention-days:180}") int retentionDays) {
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${sns.audit.prune-cron:0 30 3 * * *}")
    public void prune() {
        int total = 0;
        while (true) {
            int n = pruneOnePage();
            total += n;
            if (n < PAGE_SIZE) break;
        }
        if (total > 0) log.info("audit.prune removed {} rows older than {} days", total, retentionDays);
    }

    @Transactional
    protected int pruneOnePage() {
        em.createNativeQuery("SET LOCAL app.audit_prune = 'on'").executeUpdate();
        return em.createNativeQuery("""
                DELETE FROM audit_log
                 WHERE id IN (
                   SELECT id FROM audit_log
                    WHERE created_at < now() - make_interval(days => :days)
                    ORDER BY created_at
                    LIMIT :limit
                 )
            """)
            .setParameter("days", retentionDays)
            .setParameter("limit", PAGE_SIZE)
            .executeUpdate();
    }
}
