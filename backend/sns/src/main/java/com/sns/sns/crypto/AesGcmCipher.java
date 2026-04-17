package com.sns.sns.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM encryption for SNS OAuth tokens.
 * <p>
 * The master key is derived by SHA-256'ing {@code sns.crypto.master-key}. For production, plug in a
 * KMS-backed KeyProvider — but the API stays the same: encrypt returns {iv|ciphertext}, decrypt
 * accepts the same. IVs are 12 bytes of {@link SecureRandom}, tag is 128-bit.
 */
@Service
public class AesGcmCipher {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final byte[] key;
    private final SecureRandom rng = new SecureRandom();

    public AesGcmCipher(@Value("${sns.crypto.master-key:dev-sns-crypto-key-change-me}") String masterKey) {
        this.key = deriveKey(masterKey);
    }

    public record Encrypted(byte[] iv, byte[] ciphertext) {}

    public Encrypted encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LEN];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext);
            return new Encrypted(iv, ct);
        } catch (Exception e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    public byte[] decrypt(byte[] iv, byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt failed", e);
        }
    }

    public Encrypted encryptString(String plaintext) {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String decryptToString(byte[] iv, byte[] ciphertext) {
        return new String(decrypt(iv, ciphertext), StandardCharsets.UTF_8);
    }

    private static byte[] deriveKey(String masterKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
