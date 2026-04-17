package com.sns.matching.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "similarity_matches")
public class SimilarityMatchEntity {

    @Id
    @Column(name = "match_id")
    private UUID matchId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id_a", nullable = false)
    private UUID userIdA;

    @Column(name = "user_id_b", nullable = false)
    private UUID userIdB;

    @Column(name = "similarity", nullable = false)
    private float similarity;

    @Column(name = "common_keywords", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] commonKeywords = new String[0];

    @Column(name = "mutual", nullable = false)
    private boolean mutual;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (matchId == null) matchId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getUserIdA() { return userIdA; }
    public void setUserIdA(UUID userIdA) { this.userIdA = userIdA; }
    public UUID getUserIdB() { return userIdB; }
    public void setUserIdB(UUID userIdB) { this.userIdB = userIdB; }
    public float getSimilarity() { return similarity; }
    public void setSimilarity(float similarity) { this.similarity = similarity; }
    public String[] getCommonKeywords() { return commonKeywords; }
    public void setCommonKeywords(String[] commonKeywords) { this.commonKeywords = commonKeywords; }
    public boolean isMutual() { return mutual; }
    public void setMutual(boolean mutual) { this.mutual = mutual; }
    public OffsetDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(OffsetDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
