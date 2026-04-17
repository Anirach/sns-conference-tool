package com.sns.notification.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceTokenEntity {

    public enum Platform { ANDROID, IOS, WEB }

    @Id
    @Column(name = "token_id")
    private UUID tokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "last_seen", nullable = false)
    private OffsetDateTime lastSeen;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (tokenId == null) tokenId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastSeen == null) lastSeen = now;
    }

    public UUID getTokenId() { return tokenId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public OffsetDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(OffsetDateTime lastSeen) { this.lastSeen = lastSeen; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
