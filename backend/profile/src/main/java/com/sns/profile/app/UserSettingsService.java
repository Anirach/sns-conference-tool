package com.sns.profile.app;

import com.sns.identity.app.AuditLogger;
import com.sns.profile.api.dto.UpdateUserSettingsRequest;
import com.sns.profile.api.dto.UserSettingsDto;
import com.sns.profile.domain.UserSettingsEntity;
import com.sns.profile.repo.UserSettingsRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSettingsService {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "th", "de");

    private final UserSettingsRepository settings;
    private final AuditLogger audit;

    public UserSettingsService(UserSettingsRepository settings, AuditLogger audit) {
        this.settings = settings;
        this.audit = audit;
    }

    @Transactional
    public UserSettingsDto get(UUID userId) {
        UserSettingsEntity e = settings.findById(userId).orElseGet(() -> create(userId));
        return toDto(e);
    }

    @Transactional
    public UserSettingsDto update(UUID userId, UpdateUserSettingsRequest req) {
        UserSettingsEntity e = settings.findById(userId).orElseGet(() -> create(userId));
        if (req.pushMatches()  != null) e.setPushMatches(req.pushMatches());
        if (req.pushChat()     != null) e.setPushChat(req.pushChat());
        if (req.gpsConsent()   != null) e.setGpsConsent(req.gpsConsent());
        if (req.keepRegister() != null) e.setKeepRegister(req.keepRegister());
        if (req.language()     != null) {
            if (!SUPPORTED_LANGUAGES.contains(req.language())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported language: " + req.language());
            }
            e.setLanguage(req.language());
        }
        settings.save(e);
        audit.log("profile.settings.update", userId, "user_settings", userId.toString());
        return toDto(e);
    }

    private UserSettingsEntity create(UUID userId) {
        UserSettingsEntity e = new UserSettingsEntity();
        e.setUserId(userId);
        return settings.save(e);
    }

    private static UserSettingsDto toDto(UserSettingsEntity e) {
        return new UserSettingsDto(
            e.isPushMatches(),
            e.isPushChat(),
            e.isGpsConsent(),
            e.isKeepRegister(),
            e.getLanguage()
        );
    }
}
