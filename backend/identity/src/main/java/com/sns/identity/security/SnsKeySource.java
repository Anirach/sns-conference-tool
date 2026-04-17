package com.sns.identity.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the RS256 keypair used to sign and verify SNS-issued JWTs.
 * <p>
 * In production both keys are provided as PKCS#8 / X.509 PEM via environment. In dev, when no keys are
 * configured, an ephemeral keypair is generated at boot — fine for local work but means tokens are
 * invalidated on every restart.
 */
public final class SnsKeySource {

    private static final Logger log = LoggerFactory.getLogger(SnsKeySource.class);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    public SnsKeySource(RSAPrivateKey privateKey, RSAPublicKey publicKey, String keyId) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.keyId = keyId;
    }

    public RSAPrivateKey privateKey() { return privateKey; }
    public RSAPublicKey publicKey()   { return publicKey; }
    public String keyId()             { return keyId; }

    public static SnsKeySource fromProperties(SnsJwtProperties props) {
        if (hasText(props.privateKey()) && hasText(props.publicKey())) {
            try {
                var priv = readPrivateKey(props.privateKey());
                var pub  = readPublicKey(props.publicKey());
                return new SnsKeySource(priv, pub, "sns-rs256");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse JWT keys from configuration", e);
            }
        }
        log.warn("SNS JWT keys not configured — generating an ephemeral RS256 keypair (DEV ONLY).");
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            return new SnsKeySource(
                (RSAPrivateKey) pair.getPrivate(),
                (RSAPublicKey) pair.getPublic(),
                "sns-dev-" + UUID.randomUUID().toString().substring(0, 8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static RSAPrivateKey readPrivateKey(String pem) throws Exception {
        byte[] der = decodePem(pem, "PRIVATE KEY");
        var kf = java.security.KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey readPublicKey(String pem) throws Exception {
        byte[] der = decodePem(pem, "PUBLIC KEY");
        var kf = java.security.KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] decodePem(String pem, String label) {
        String stripped = pem
            .replace("-----BEGIN " + label + "-----", "")
            .replace("-----END " + label + "-----", "")
            .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(stripped);
    }
}
