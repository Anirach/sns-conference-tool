package com.sns.profile.api;

import com.sns.profile.api.dto.UpdateProfileRequest;
import com.sns.profile.api.dto.UserDto;
import com.sns.profile.app.ProfileService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public UserDto me(JwtAuthenticationToken auth) {
        return service.get(currentUserId(auth));
    }

    @PutMapping
    public UserDto update(JwtAuthenticationToken auth, @RequestBody UpdateProfileRequest req) {
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
