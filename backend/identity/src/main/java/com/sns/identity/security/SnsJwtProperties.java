package com.sns.identity.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sns.jwt")
public record SnsJwtProperties(
    String issuer,
    String audience,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    Duration clockSkew,
    String privateKey,
    String publicKey
) {}
