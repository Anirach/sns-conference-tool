package com.sns.notification.app;

import com.sns.common.events.ChatMessageSent;
import com.sns.common.events.MatchFound;
import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.PushOutboxEntity;
import com.sns.notification.domain.PushOutboxEntity.Status;
import com.sns.notification.repo.DeviceTokenRepository;
import com.sns.notification.repo.PushOutboxRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists pending push notifications into {@code push_outbox} and drains them on a schedule
 * with at-least-once semantics. Domain events (match found, chat message sent) convert into
 * outbox rows synchronously so a crash between persist and deliver never loses a notification;
 * duplicates are acceptable because device tokens tolerate them.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_ATTEMPTS = 5;

    private final DeviceTokenRepository devices;
    private final PushOutboxRepository outbox;
    private final PushGateway gateway;

    public NotificationService(
        DeviceTokenRepository devices,
        PushOutboxRepository outbox,
        PushGateway gateway
    ) {
        this.devices = devices;
        this.outbox = outbox;
        this.gateway = gateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchFound(MatchFound event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.eventId().toString());
        payload.put("matchId", event.matchId().toString());
        payload.put("similarity", event.similarity());
        payload.put("commonKeywords", event.commonKeywords());
        enqueue(event.userIdA(), "match.found", payload);
        enqueue(event.userIdB(), "match.found", payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageSent(ChatMessageSent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.eventId().toString());
        payload.put("fromUserId", event.fromUserId().toString());
        payload.put("messageId", event.messageId().toString());
        payload.put("preview", shorten(event.content()));
        enqueue(event.toUserId(), "chat.message", payload);
    }

    @Transactional
    public PushOutboxEntity enqueue(UUID userId, String kind, Map<String, Object> payload) {
        var row = new PushOutboxEntity();
        row.setUserId(userId);
        row.setKind(kind);
        row.setPayload(payload);
        row.setStatus(Status.PENDING);
        return outbox.save(row);
    }

    @Scheduled(fixedDelayString = "${sns.push.drain-interval-ms:5000}")
    public void drain() {
        List<PushOutboxEntity> batch = outbox.findByStatus(Status.PENDING, PageRequest.of(0, 50));
        for (var row : batch) {
            deliverOne(row);
        }
    }

    @Transactional
    protected void deliverOne(PushOutboxEntity row) {
        List<DeviceTokenEntity> userDevices = devices.findByUserId(row.getUserId());
        if (userDevices.isEmpty()) {
            // Ship without a device — mark delivered so we don't retry forever. A real system would
            // have a separate email/web path; for now the WS broadcast is the user-facing channel.
            row.setStatus(Status.DELIVERED);
            row.setDeliveredAt(OffsetDateTime.now());
            outbox.save(row);
            return;
        }
        boolean anySuccess = false;
        String lastError = null;
        for (var d : userDevices) {
            try {
                gateway.deliver(d, row.getKind(), row.getPayload());
                anySuccess = true;
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("push deliver failed user={} kind={} err={}", row.getUserId(), row.getKind(), lastError);
            }
        }
        row.setAttempts((short) (row.getAttempts() + 1));
        if (anySuccess) {
            row.setStatus(Status.DELIVERED);
            row.setDeliveredAt(OffsetDateTime.now());
        } else if (row.getAttempts() >= MAX_ATTEMPTS) {
            row.setStatus(Status.FAILED);
            row.setLastError(lastError);
        } else {
            row.setLastError(lastError);
        }
        outbox.save(row);
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 117) + "..." : s;
    }
}
