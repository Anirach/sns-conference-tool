package com.sns.notification.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "push_outbox")
public class PushOutboxEntity {

    public enum Status { PENDING, DELIVERED, FAILED }

    @Id
    @Column(name = "outbox_id")
    private UUID outboxId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @PrePersist
    void prePersist() {
        if (outboxId == null) outboxId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getOutboxId() { return outboxId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public short getAttempts() { return attempts; }
    public void setAttempts(short attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
