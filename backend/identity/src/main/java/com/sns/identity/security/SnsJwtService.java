package com.sns.identity.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sns.identity.domain.Role;
import com.sns.identity.repo.UserRepository;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Signs SNS-issued access tokens. Refresh tokens are opaque UUIDs stored server-side
 * (see {@link RefreshTokenService}) — only access tokens carry JWT claims.
 */
@Service
public class SnsJwtService {

    private final SnsKeySource keys;
    private final SnsJwtProperties props;
    private final UserRepository users;

    public SnsJwtService(SnsKeySource keys, SnsJwtProperties props, UserRepository users) {
        this.keys = keys;
        this.props = props;
        this.users = users;
    }

    public String issueAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.accessTokenTtl());
        // Look up role to embed as a claim — admin matchers in SecurityConfig authorise off this.
        // For unknown / deleted users we default to USER (the JWT will still be rejected later
        // because the userId won't resolve in the resource server's principal lookup).
        Role role = users.findById(userId).map(u -> u.getRole() == null ? Role.USER : u.getRole())
            .orElse(Role.USER);
        try {
            var builder = new JWTClaimsSet.Builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("role", role.name());
            if (props.audience() != null && !props.audience().isBlank()) {
                builder.audience(props.audience());
            }
            JWTClaimsSet claims = builder.build();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keys.keyId())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(keys.privateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }
}
