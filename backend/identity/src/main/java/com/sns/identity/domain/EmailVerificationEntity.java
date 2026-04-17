package com.sns.identity.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
public class EmailVerificationEntity {

    @Id
    @Column(name = "verification_id")
    private UUID verificationId;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "tan_hash", nullable = false)
    private String tanHash;

    @Column(name = "verification_token")
    private UUID verificationToken;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (verificationId == null) verificationId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getVerificationId() { return verificationId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTanHash() { return tanHash; }
    public void setTanHash(String tanHash) { this.tanHash = tanHash; }
    public UUID getVerificationToken() { return verificationToken; }
    public void setVerificationToken(UUID verificationToken) { this.verificationToken = verificationToken; }
    public OffsetDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(OffsetDateTime consumedAt) { this.consumedAt = consumedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
