package com.sns.app.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Halts boot if the prod profile is active and any of the security-critical secrets is still set
 * to its dev default. The single biggest foot-gun in this codebase is shipping a Helm release
 * that forgot to wire {@code SNS_QR_HMAC_KEY} or {@code JWT_PRIVATE_KEY}; we'd rather refuse to
 * start than silently sign QR tokens with {@code dev-hmac-key-change-me}.
 *
 * <p>Only active under the {@code prod} profile. Staging is up to the operator (some staging
 * setups deliberately run with rotating ephemeral keys).
 */
@Component
@Profile("prod")
public class ProductionSecretsCheck {

    private static final String QR_DEFAULT = "dev-hmac-key-change-me";
    private static final String CRYPTO_DEFAULT = "dev-sns-crypto-key-change-me";
    private static final String AUDIT_SALT_DEFAULT = "dev-audit-ip-salt-change-me";

    private final String qrHmac;
    private final String cryptoMaster;
    private final String auditIpSalt;
    private final String jwtPrivate;
    private final String jwtPublic;
    private final boolean seedDemoData;

    public ProductionSecretsCheck(
        @Value("${sns.qr.hmac-key:}") String qrHmac,
        @Value("${sns.crypto.master-key:}") String cryptoMaster,
        @Value("${sns.audit.ip-salt:}") String auditIpSalt,
        @Value("${sns.jwt.private-key:}") String jwtPrivate,
        @Value("${sns.jwt.public-key:}") String jwtPublic,
        @Value("${sns.dev.seed-demo-data:false}") boolean seedDemoData
    ) {
        this.qrHmac = qrHmac;
        this.cryptoMaster = cryptoMaster;
        this.auditIpSalt = auditIpSalt;
        this.jwtPrivate = jwtPrivate;
        this.jwtPublic = jwtPublic;
        this.seedDemoData = seedDemoData;
    }

    @PostConstruct
    void verify() {
        List<String> offenders = new ArrayList<>();
        if (qrHmac.isBlank() || QR_DEFAULT.equals(qrHmac)) offenders.add("sns.qr.hmac-key");
        if (cryptoMaster.isBlank() || CRYPTO_DEFAULT.equals(cryptoMaster)) offenders.add("sns.crypto.master-key");
        if (auditIpSalt.isBlank() || AUDIT_SALT_DEFAULT.equals(auditIpSalt)) offenders.add("sns.audit.ip-salt");
        if (jwtPrivate.isBlank() || jwtPublic.isBlank()) {
            // Ephemeral keypair in prod = every restart invalidates every token. Fatal.
            offenders.add("sns.jwt.private-key/public-key (ephemeral keypair forbidden in prod)");
        }
        if (seedDemoData) {
            // Inserts 21 fixture users with the well-known seed password — must never run in prod.
            offenders.add("sns.dev.seed-demo-data (set to false in prod; seeds fixture users)");
        }
        if (!offenders.isEmpty()) {
            throw new BeanInitializationException(
                "Refusing to start with default / unset secrets in prod: " + String.join(", ", offenders)
                + " — see docs/SECURITY.md for rotation procedures."
            );
        }
    }
}
