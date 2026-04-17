package com.sns.sns.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "sns_links")
public class SnsLinkEntity {

    public enum Provider { FACEBOOK, LINKEDIN }

    @Id
    @Column(name = "sns_id")
    private UUID snsId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "access_token_enc")
    private byte[] accessTokenEnc;

    @Column(name = "refresh_token_enc")
    private byte[] refreshTokenEnc;

    @Column(name = "token_iv")
    private byte[] tokenIv;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "last_fetch")
    private OffsetDateTime lastFetch;

    @Type(JsonType.class)
    @Column(name = "imported_data", columnDefinition = "jsonb")
    private Map<String, Object> importedData;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (snsId == null) snsId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getSnsId() { return snsId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public byte[] getAccessTokenEnc() { return accessTokenEnc; }
    public void setAccessTokenEnc(byte[] accessTokenEnc) { this.accessTokenEnc = accessTokenEnc; }
    public byte[] getRefreshTokenEnc() { return refreshTokenEnc; }
    public void setRefreshTokenEnc(byte[] refreshTokenEnc) { this.refreshTokenEnc = refreshTokenEnc; }
    public byte[] getTokenIv() { return tokenIv; }
    public void setTokenIv(byte[] tokenIv) { this.tokenIv = tokenIv; }
    public OffsetDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(OffsetDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public OffsetDateTime getLastFetch() { return lastFetch; }
    public void setLastFetch(OffsetDateTime lastFetch) { this.lastFetch = lastFetch; }
    public Map<String, Object> getImportedData() { return importedData; }
    public void setImportedData(Map<String, Object> importedData) { this.importedData = importedData; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
