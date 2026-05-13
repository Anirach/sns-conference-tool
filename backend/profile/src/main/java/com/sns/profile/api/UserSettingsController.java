package com.sns.profile.api;

import com.sns.profile.api.dto.UpdateUserSettingsRequest;
import com.sns.profile.api.dto.UserSettingsDto;
import com.sns.profile.app.UserSettingsService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile/settings")
public class UserSettingsController {

    private final UserSettingsService service;

    public UserSettingsController(UserSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public UserSettingsDto get(JwtAuthenticationToken auth) {
        return service.get(currentUserId(auth));
    }

    @PutMapping
    public UserSettingsDto update(JwtAuthenticationToken auth, @RequestBody UpdateUserSettingsRequest req) {
        return service.update(currentUserId(auth), req);
    }

    private static UUID currentUserId(JwtAuthenticationToken auth) {
        Jwt token = auth.getToken();
        try {
            return UUID.fromString(token.getSubject());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject");
        }
    }
}
