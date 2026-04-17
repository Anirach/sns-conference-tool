package com.sns.event.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "venue")
    private String venue;

    @Column(name = "expiration_code", nullable = false)
    private OffsetDateTime expirationCode;

    @Column(name = "qr_code_hash", nullable = false, unique = true)
    private String qrCodeHash;

    @Column(name = "qr_code_plaintext")
    private String qrCodePlaintext;

    @Column(name = "centroid", columnDefinition = "geography(Point,4326)")
    private Point centroid;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (eventId == null) eventId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public OffsetDateTime getExpirationCode() { return expirationCode; }
    public void setExpirationCode(OffsetDateTime expirationCode) { this.expirationCode = expirationCode; }
    public String getQrCodeHash() { return qrCodeHash; }
    public void setQrCodeHash(String qrCodeHash) { this.qrCodeHash = qrCodeHash; }
    public String getQrCodePlaintext() { return qrCodePlaintext; }
    public void setQrCodePlaintext(String qrCodePlaintext) { this.qrCodePlaintext = qrCodePlaintext; }
    public Point getCentroid() { return centroid; }
    public void setCentroid(Point centroid) { this.centroid = centroid; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isExpired() {
        return expirationCode != null && expirationCode.isBefore(OffsetDateTime.now());
    }
}
