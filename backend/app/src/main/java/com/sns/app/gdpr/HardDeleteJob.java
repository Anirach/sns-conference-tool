package com.sns.app.gdpr;

import com.sns.identity.domain.UserEntity;
import com.sns.identity.repo.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes soft-deleted users to hard-deleted after a configurable grace period (default 30 days).
 * Cascades automatically via FK ON DELETE CASCADE on profiles, interests, participations, matches,
 * chat messages, device tokens, refresh tokens, and SNS links.
 */
@Component
public class HardDeleteJob {

    private static final Logger log = LoggerFactory.getLogger(HardDeleteJob.class);

    private final UserRepository users;
    private final int graceDays;

    public HardDeleteJob(
        UserRepository users,
        @Value("${sns.gdpr.hard-delete-grace-days:30}") int graceDays
    ) {
        this.users = users;
        this.graceDays = graceDays;
    }

    @Scheduled(cron = "${sns.gdpr.hard-delete-cron:0 0 3 * * *}")
    @Transactional
    public void run() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(graceDays);
        List<UserEntity> candidates = users.findAll().stream()
            .filter(u -> u.getDeletedAt() != null && u.getDeletedAt().isBefore(cutoff))
            .toList();
        for (UserEntity u : candidates) {
            log.info("gdpr.hard_delete userId={} deletedAt={}", u.getUserId(), u.getDeletedAt());
            users.delete(u);
        }
    }
}
