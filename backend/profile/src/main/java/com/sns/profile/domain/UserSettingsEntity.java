package com.sns.profile.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "push_matches", nullable = false)
    private boolean pushMatches = true;

    @Column(name = "push_chat", nullable = false)
    private boolean pushChat = true;

    @Column(name = "gps_consent", nullable = false)
    private boolean gpsConsent = true;

    @Column(name = "keep_register", nullable = false)
    private boolean keepRegister = false;

    @Column(name = "language", nullable = false, length = 8)
    private String language = "en";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public boolean isPushMatches() { return pushMatches; }
    public void setPushMatches(boolean pushMatches) { this.pushMatches = pushMatches; }
    public boolean isPushChat() { return pushChat; }
    public void setPushChat(boolean pushChat) { this.pushChat = pushChat; }
    public boolean isGpsConsent() { return gpsConsent; }
    public void setGpsConsent(boolean gpsConsent) { this.gpsConsent = gpsConsent; }
    public boolean isKeepRegister() { return keepRegister; }
    public void setKeepRegister(boolean keepRegister) { this.keepRegister = keepRegister; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
