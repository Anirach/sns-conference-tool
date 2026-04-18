package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.event.repo.EventRepository;
import com.sns.identity.repo.AuditLogRepository;
import com.sns.identity.repo.UserRepository;
import com.sns.matching.repo.SimilarityMatchRepository;
import com.sns.notification.domain.PushOutboxEntity;
import com.sns.notification.repo.PushOutboxRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminOpsService {

    private final UserRepository users;
    private final EventRepository events;
    private final PushOutboxRepository outbox;
    private final SimilarityMatchRepository matches;
    private final AuditLogRepository audit;

    public AdminOpsService(
        UserRepository users, EventRepository events,
        PushOutboxRepository outbox, SimilarityMatchRepository matches,
        AuditLogRepository audit
    ) {
        this.users = users; this.events = events; this.outbox = outbox;
        this.matches = matches; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AdminDtos.Page<AdminDtos.OutboxRow> outbox(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        var p = (status == null || status.isBlank())
            ? outbox.findAllByOrderByCreatedAtDesc(pageable)
            : outbox.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        var items = p.getContent().stream()
            .map(o -> new AdminDtos.OutboxRow(
                o.getOutboxId(), o.getUserId(), o.getKind(), o.getStatus().name(),
                o.getAttempts(), o.getLastError(), o.getCreatedAt(), o.getDeliveredAt()))
            .toList();
        return new AdminDtos.Page<>(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public void retry(UUID outboxId) {
        PushOutboxEntity row = outbox.findById(outboxId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        row.setStatus(PushOutboxEntity.Status.PENDING);
        row.setAttempts((short) 0);
        row.setLastError(null);
        row.setDeliveredAt(null);
        outbox.save(row);
    }

    @Transactional(readOnly = true)
    public AdminDtos.OpsMetrics metrics() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime dayAgo = now.minusDays(1);
        return new AdminDtos.OpsMetrics(
            new AdminDtos.OpsMetrics.UserMetrics(
                users.count(),
                users.countByDeletedAtIsNullAndSuspendedAtIsNull(),
                users.countBySuspendedAtIsNotNullAndDeletedAtIsNull(),
                users.countByDeletedAtAfter(dayAgo)),
            new AdminDtos.OpsMetrics.EventMetrics(
                events.countByExpirationCodeAfter(now),
                events.countByExpirationCodeBefore(now)),
            new AdminDtos.OpsMetrics.OutboxMetrics(
                outbox.countByStatus(PushOutboxEntity.Status.PENDING),
                outbox.countByStatus(PushOutboxEntity.Status.FAILED),
                outbox.countByStatusAndDeliveredAtAfter(PushOutboxEntity.Status.DELIVERED, dayAgo)),
            new AdminDtos.OpsMetrics.MatchMetrics(
                matches.count(),
                matches.countByCreatedAtAfter(dayAgo)),
            audit.countByCreatedAtAfter(dayAgo));
    }

    private static PushOutboxEntity.Status parseStatus(String s) {
        try {
            return PushOutboxEntity.Status.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "status must be PENDING|DELIVERED|FAILED");
        }
    }
}
