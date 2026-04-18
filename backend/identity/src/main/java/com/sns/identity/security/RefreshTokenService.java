package com.sns.identity.security;

import com.sns.identity.domain.RefreshTokenEntity;
import com.sns.identity.repo.RefreshTokenRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens are opaque UUIDs ({@code jti}) backed by a row in {@code refresh_tokens}. Rotation
 * (one-time use) is enforced: presenting a token both issues a new one and marks the old one
 * {@code revoked=true} with {@code replaced_by} pointing to the successor.
 *
 * <p>Reuse detection: presenting an already-revoked token is the canonical signal of token theft.
 * In response we walk the {@code replaced_by} chain forward and revoke every descendant, then
 * revoke every other live token belonging to the same user. This forces both the attacker and the
 * legitimate client back through the password-login path. The triggering call still fails with
 * {@link InvalidRefreshTokenException}.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

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
            // Theft signal — revoke entire family belonging to this user, including descendants.
            log.warn("Refresh-token reuse detected for user {} (jti {}); revoking session family",
                current.getUserId(), presentedJti);
            revokeFamily(current);
            throw new InvalidRefreshTokenException("token reused");
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

    /**
     * Walk forward through the rotation chain and revoke every successor, then revoke every other
     * live refresh token for this user as a belt-and-braces measure.
     */
    @Transactional
    protected void revokeFamily(RefreshTokenEntity startedAt) {
        UUID userId = startedAt.getUserId();
        // Forward chain.
        var cursor = startedAt;
        int safetyHops = 100;
        while (cursor != null && safetyHops-- > 0) {
            UUID nextId = cursor.getReplacedBy();
            if (nextId == null) break;
            cursor = repo.findById(nextId).orElse(null);
            if (cursor != null && !cursor.isRevoked()) {
                cursor.setRevoked(true);
                repo.save(cursor);
            }
        }
        // Any other live tokens for this user.
        for (RefreshTokenEntity t : repo.findByUserIdAndRevokedFalse(userId)) {
            t.setRevoked(true);
            repo.save(t);
        }
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String msg) { super(msg); }
    }
}
