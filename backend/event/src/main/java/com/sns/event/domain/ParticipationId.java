package com.sns.event.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ParticipationId implements Serializable {

    private UUID userId;
    private UUID eventId;

    public ParticipationId() {}

    public ParticipationId(UUID userId, UUID eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipationId other)) return false;
        return Objects.equals(userId, other.userId) && Objects.equals(eventId, other.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, eventId);
    }
}
