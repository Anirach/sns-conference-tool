package com.sns.identity.security;

import com.sns.identity.domain.RefreshTokenEntity;
import com.sns.identity.repo.RefreshTokenRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens are opaque UUIDs ({@code jti}) backed by a row in {@code refresh_tokens}. Rotation
 * (one-time use) is enforced: presenting a token both issues a new one and marks the old one
 * {@code revoked=true} with {@code replaced_by} pointing to the successor.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final SnsJwtProperties props;

    public RefreshTokenService(RefreshTokenRepository repo, SnsJwtProperties props) {
        this.repo = repo;
        this.props = props;
    }

    @Transactional
    public RefreshTokenEntity issue(UUID userId) {
        var token = new RefreshTokenEntity();
        token.setJti(UUID.randomUUID());
        token.setUserId(userId);
        token.setExpiresAt(OffsetDateTime.now().plus(props.refreshTokenTtl()));
        token.setRevoked(false);
        return repo.save(token);
    }

    @Transactional
    public RefreshTokenEntity rotate(UUID presentedJti) {
        RefreshTokenEntity current = repo.findById(presentedJti)
            .orElseThrow(() -> new InvalidRefreshTokenException("unknown jti"));
        if (current.isRevoked()) {
            throw new InvalidRefreshTokenException("token revoked");
        }
        if (current.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException("token expired");
        }
        RefreshTokenEntity next = issue(current.getUserId());
        current.setRevoked(true);
        current.setReplacedBy(next.getJti());
        repo.save(current);
        return next;
    }

    @Transactional
    public void revoke(UUID jti) {
        repo.findById(jti).ifPresent(t -> {
            t.setRevoked(true);
            repo.save(t);
        });
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String msg) { super(msg); }
    }
}
