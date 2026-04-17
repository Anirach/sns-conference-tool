package com.sns.event.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "participations")
@IdClass(ParticipationId.class)
public class ParticipationEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "selected_radius", nullable = false)
    private short selectedRadius = 50;

    @Column(name = "last_position", columnDefinition = "geography(Point,4326)")
    private Point lastPosition;

    @Column(name = "last_position_acc_m")
    private Float lastPositionAccM;

    @Column(name = "last_update")
    private OffsetDateTime lastUpdate;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) joinedAt = OffsetDateTime.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public short getSelectedRadius() { return selectedRadius; }
    public void setSelectedRadius(short selectedRadius) { this.selectedRadius = selectedRadius; }
    public Point getLastPosition() { return lastPosition; }
    public void setLastPosition(Point lastPosition) { this.lastPosition = lastPosition; }
    public Float getLastPositionAccM() { return lastPositionAccM; }
    public void setLastPositionAccM(Float lastPositionAccM) { this.lastPositionAccM = lastPositionAccM; }
    public OffsetDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(OffsetDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(OffsetDateTime joinedAt) { this.joinedAt = joinedAt; }
}
