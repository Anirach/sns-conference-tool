package com.sns.notification.api;

import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.DeviceTokenEntity.Platform;
import com.sns.notification.repo.DeviceTokenRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    public record RegisterRequest(@NotNull Platform platform, @NotBlank String token, String appVersion) {}

    private final DeviceTokenRepository repo;

    public DeviceController(DeviceTokenRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/register")
    @Transactional
    public Map<String, Object> register(JwtAuthenticationToken auth, @RequestBody RegisterRequest req) {
        UUID userId = UUID.fromString(auth.getToken().getSubject());
        var existing = repo.findByUserIdAndToken(userId, req.token()).orElseGet(DeviceTokenEntity::new);
        existing.setUserId(userId);
        existing.setPlatform(req.platform());
        existing.setToken(req.token());
        existing.setAppVersion(req.appVersion());
        existing.setLastSeen(OffsetDateTime.now());
        repo.save(existing);
        return Map.of("ok", true);
    }
}
