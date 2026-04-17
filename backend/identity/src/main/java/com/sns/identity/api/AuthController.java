package com.sns.identity.api;

import com.sns.identity.api.dto.AuthDtos.*;
import com.sns.identity.app.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        auth.startRegistration(req.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RegisterResponse(true));
    }

    @PostMapping("/verify")
    public VerifyResponse verify(@Valid @RequestBody VerifyRequest req) {
        return auth.verifyTan(req.email(), req.tan());
    }

    @PostMapping("/complete")
    public AuthTokens complete(@Valid @RequestBody CompleteRequest req) {
        return auth.completeRegistration(req);
    }

    @PostMapping("/login")
    public AuthTokens login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password());
    }

    @PostMapping("/refresh")
    public AuthTokens refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody(required = false) RefreshRequest req) {
        if (req != null && req.refreshToken() != null) {
            auth.logout(req.refreshToken());
        }
        return Map.of("ok", true);
    }
}
