package com.sns.sns.api;

import com.sns.sns.app.SnsOauthConfig;
import com.sns.sns.app.SnsService;
import com.sns.sns.domain.SnsLinkEntity.Provider;
import jakarta.validation.constraints.NotBlank;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sns")
public class SnsController {

    public record LinkRequest(@NotBlank String provider) {}
    public record LinkResponse(String authUrl, String state) {}
    public record CallbackRequest(@NotBlank String provider, @NotBlank String code, @NotBlank String state) {}

    private final SnsOauthConfig cfg;
    private final SnsService service;

    public SnsController(SnsOauthConfig cfg, SnsService service) {
        this.cfg = cfg;
        this.service = service;
    }

    @GetMapping
    public List<?> list(JwtAuthenticationToken auth) {
        return service.list(userId(auth));
    }

    @PostMapping("/link")
    public LinkResponse beginLink(JwtAuthenticationToken auth, @RequestBody LinkRequest req) {
        var client = cfg.client(req.provider());
        if (client == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown provider");
        if (!client.isConfigured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "provider not configured");

        String state = UUID.randomUUID().toString();
        String url = client.authUri()
            + "?response_type=code"
            + "&client_id=" + URLEncoder.encode(client.clientId(), StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(client.redirectUri(), StandardCharsets.UTF_8)
            + "&scope=" + URLEncoder.encode(client.scopes(), StandardCharsets.UTF_8)
            + "&state=" + state;
        service.rememberPendingState(userId(auth), Provider.valueOf(req.provider().toUpperCase()), state);
        return new LinkResponse(url, state);
    }

    @PostMapping("/callback")
    public Map<String, Object> callback(JwtAuthenticationToken auth, @RequestBody CallbackRequest req) {
        var provider = Provider.valueOf(req.provider().toUpperCase());
        service.completeLink(userId(auth), provider, req.code(), req.state());
        return Map.of("ok", true);
    }

    @DeleteMapping("/{provider}")
    public Map<String, Object> unlink(JwtAuthenticationToken auth, @PathVariable String provider) {
        service.unlink(userId(auth), Provider.valueOf(provider.toUpperCase()));
        return Map.of("ok", true);
    }

    private static UUID userId(JwtAuthenticationToken auth) {
        return UUID.fromString(auth.getToken().getSubject());
    }
}
