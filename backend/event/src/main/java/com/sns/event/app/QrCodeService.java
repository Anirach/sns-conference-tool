package com.sns.event.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Hashing and signing for event QR codes. Two forms are supported on the join path:
 * <ol>
 *   <li><b>Legacy hash</b> — {@code SHA-256(upper(plaintext))} stored against the event row.
 *       Short, human-typable; works forever as long as we keep the hash around.</li>
 *   <li><b>Signed token</b> — {@code base32url(eventCode|expiresAt) + "." + base32url(hmac)}
 *       with an embedded expiry. Unspoofable even if the database leaks; used for printed badges
 *       where we don't want the plaintext on the wire.</li>
 * </ol>
 * Callers (event join) try the signed form first, then fall back to hash lookup.
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

    public String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Issues a signed token. The encoded payload is {@code eventCode|expiresAtSeconds}; the
     * signature is HMAC-SHA256 over the base32url payload bytes.
     */
    public String issue(String eventCode, Instant expiresAt) {
        String payload = eventCode.trim().toUpperCase() + "|" + expiresAt.getEpochSecond();
        String encoded = base32(payload.getBytes(StandardCharsets.UTF_8));
        String sig = base32(macBytes(encoded.getBytes(StandardCharsets.US_ASCII)));
        return encoded + "." + sig;
    }

    /**
     * Verifies a signed token and returns the plaintext event code.
     * @throws IllegalArgumentException on bad format, bad signature, or expired token.
     */
    public String verify(String token) {
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            throw new IllegalArgumentException("Malformed signed QR token");
        }
        String encoded = token.substring(0, dot);
        String sig = token.substring(dot + 1);
        String expected = base32(macBytes(encoded.getBytes(StandardCharsets.US_ASCII)));
        if (!constantTimeEquals(sig, expected)) {
            throw new IllegalArgumentException("Bad QR token signature");
        }
        String payload = new String(unbase32(encoded), StandardCharsets.UTF_8);
        int sep = payload.indexOf('|');
        if (sep < 0) throw new IllegalArgumentException("Malformed QR token payload");
        String eventCode = payload.substring(0, sep);
        long exp;
        try {
            exp = Long.parseLong(payload.substring(sep + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed QR token expiry");
        }
        if (Instant.now().getEpochSecond() > exp) {
            throw new IllegalArgumentException("QR token expired");
        }
        return eventCode;
    }

    /** Attempts {@link #verify(String)}; returns null on any failure. Useful for dual-format lookup. */
    public String tryVerify(String token) {
        try {
            return verify(token);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private byte[] macBytes(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base32(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] unbase32(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
