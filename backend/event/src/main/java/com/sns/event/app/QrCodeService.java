package com.sns.event.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Hashing for event QR codes. The plaintext code printed on the badge is never stored directly;
 * we store {@code SHA-256(plaintext)} and compare on join. Future: move to HMAC-signed tokens
 * {@code base32(payload) + "." + base32(hmac(secret, payload))} with an embedded expiry so that
 * QRs are unspoofable even if the database is leaked.
 */
@Service
public class QrCodeService {

    private final byte[] hmacKey;

    public QrCodeService(@Value("${sns.qr.hmac-key:dev-hmac-key-change-me}") String hmacKey) {
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String plaintext) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(plaintext.trim().toUpperCase().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** HMAC over arbitrary payload — used by Phase 3+ for signed QR tokens. */
    public String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
