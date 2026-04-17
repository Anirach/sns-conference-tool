package com.sns.identity.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID jti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getJti() { return jti; }
    public void setJti(UUID jti) { this.jti = jti; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public UUID getReplacedBy() { return replacedBy; }
    public void setReplacedBy(UUID replacedBy) { this.replacedBy = replacedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
