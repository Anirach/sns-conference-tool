package com.sns.sns.app;

import com.sns.sns.crypto.AesGcmCipher;
import com.sns.sns.domain.SnsLinkEntity;
import com.sns.sns.domain.SnsLinkEntity.Provider;
import com.sns.sns.repo.SnsLinkRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Link / unlink flow for Facebook and LinkedIn. In dev-mode, when the provider isn't configured, a
 * fake token exchange is performed so the web UI can still demo the flow — the linked row has
 * provider_user_id = "demo-<userId-prefix>" and no stored access token.
 */
@Service
public class SnsService {

    private static final Logger log = LoggerFactory.getLogger(SnsService.class);
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final SnsLinkRepository repo;
    private final SnsOauthConfig cfg;
    private final AesGcmCipher cipher;

    private final ConcurrentMap<String, PendingState> pendingStates = new ConcurrentHashMap<>();

    public SnsService(SnsLinkRepository repo, SnsOauthConfig cfg, AesGcmCipher cipher) {
        this.repo = repo;
        this.cfg = cfg;
        this.cipher = cipher;
    }

    public record PendingState(UUID userId, Provider provider, OffsetDateTime expiresAt) {}

    public record LinkDto(UUID snsId, String provider, String providerUserId, String linkedAt) {}

    public void rememberPendingState(UUID userId, Provider provider, String state) {
        pendingStates.put(state, new PendingState(userId, provider, OffsetDateTime.now().plus(STATE_TTL)));
    }

    public List<LinkDto> list(UUID userId) {
        return repo.findByUserId(userId).stream()
            .map(l -> new LinkDto(
                l.getSnsId(),
                l.getProvider().name(),
                l.getProviderUserId(),
                l.getCreatedAt().toString()
            )).toList();
    }

    @Transactional
    public void completeLink(UUID userId, Provider provider, String code, String state) {
        var pending = pendingStates.remove(state);
        if (pending == null || pending.expiresAt().isBefore(OffsetDateTime.now())
            || !pending.userId().equals(userId) || pending.provider() != provider) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid or expired state");
        }

        var client = cfg.client(provider.name());
        TokenResponse tokens;
        String providerUserId;
        if (!client.isConfigured()) {
            // Dev fallback: fake a successful exchange so the full flow can be demo'd without secrets.
            log.warn("SNS provider {} not configured — completing link with a stub token", provider);
            tokens = new TokenResponse("stub-access-token-" + UUID.randomUUID(), null, null);
            providerUserId = "demo-" + userId.toString().substring(0, 8);
        } else {
            tokens = exchangeCode(client, code);
            providerUserId = lookupProviderUserId(provider, tokens.accessToken());
        }

        SnsLinkEntity row = repo.findByUserIdAndProvider(userId, provider).orElseGet(SnsLinkEntity::new);
        row.setUserId(userId);
        row.setProvider(provider);
        row.setProviderUserId(providerUserId);

        var enc = cipher.encryptString(tokens.accessToken());
        row.setAccessTokenEnc(enc.ciphertext());
        row.setTokenIv(enc.iv());
        if (tokens.refreshToken() != null) {
            var encR = cipher.encryptString(tokens.refreshToken());
            row.setRefreshTokenEnc(encR.ciphertext());
        }
        if (tokens.expiresInSeconds() != null) {
            row.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(tokens.expiresInSeconds()));
        }
        repo.save(row);
    }

    @Transactional
    public void unlink(UUID userId, Provider provider) {
        repo.findByUserIdAndProvider(userId, provider).ifPresent(repo::delete);
    }

    /** Decrypt-on-read for background enrichment jobs. */
    public String readAccessToken(SnsLinkEntity link) {
        if (link.getAccessTokenEnc() == null || link.getTokenIv() == null) return null;
        return cipher.decryptToString(link.getTokenIv(), link.getAccessTokenEnc());
    }

    record TokenResponse(String accessToken, String refreshToken, Long expiresInSeconds) {}

    @SuppressWarnings("unchecked")
    private TokenResponse exchangeCode(SnsOauthConfig.Client client, String code) {
        try {
            RestClient rest = RestClient.create();
            var body = UriComponentsBuilder.newInstance()
                .queryParam("grant_type", "authorization_code")
                .queryParam("code", code)
                .queryParam("redirect_uri", client.redirectUri())
                .queryParam("client_id", client.clientId())
                .queryParam("client_secret", client.clientSecret())
                .build().toUriString().substring(1);
            Map<String, Object> json = rest.post()
                .uri(client.tokenUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(body)
                .retrieve()
                .body(Map.class);
            if (json == null || json.get("access_token") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "no access_token in provider response");
            }
            Long exp = null;
            if (json.get("expires_in") instanceof Number n) exp = n.longValue();
            return new TokenResponse(
                (String) json.get("access_token"),
                (String) json.get("refresh_token"),
                exp
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "token exchange failed: " + e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String lookupProviderUserId(Provider provider, String accessToken) {
        RestClient rest = RestClient.create();
        String url = switch (provider) {
            case FACEBOOK -> "https://graph.facebook.com/v19.0/me?fields=id";
            case LINKEDIN -> "https://api.linkedin.com/v2/userinfo";
        };
        try {
            Map json = rest.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
            if (json == null) return "unknown";
            Object id = json.getOrDefault("id", json.get("sub"));
            return id == null ? "unknown" : id.toString();
        } catch (Exception e) {
            log.warn("provider userinfo lookup failed: {}", e.toString());
            return "unknown";
        }
    }

    public Map<String, Object> buildExport(UUID userId) {
        Map<String, Object> out = new HashMap<>();
        out.put("links", list(userId));
        return out;
    }
}
