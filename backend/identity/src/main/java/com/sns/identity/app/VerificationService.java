package com.sns.identity.app;

import com.sns.identity.domain.EmailVerificationEntity;
import com.sns.identity.repo.EmailVerificationRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and verifies 6-digit TANs for email verification.
 * <p>
 * In dev-mode the TAN is always {@code 123456} so Playwright / curl flows work without a mail
 * inbox. In prod-mode a cryptographically random TAN is generated and the hashed value is stored;
 * the plaintext is emailed out via {@link VerificationMailSender}.
 */
@Service
public class VerificationService {

    private final EmailVerificationRepository repo;
    private final VerificationMailSender mail;
    private final boolean devMode;
    private final Duration tanTtl;

    public VerificationService(
        EmailVerificationRepository repo,
        VerificationMailSender mail,
        @Value("${sns.verification.dev-mode:true}") boolean devMode,
        @Value("${sns.verification.tan-ttl:PT15M}") Duration tanTtl
    ) {
        this.repo = repo;
        this.mail = mail;
        this.devMode = devMode;
        this.tanTtl = tanTtl;
    }

    @Transactional
    public void startVerification(String email) {
        String tan = devMode ? "123456" : randomSixDigits();
        var ev = new EmailVerificationEntity();
        ev.setEmail(email);
        ev.setTanHash(hash(tan));
        ev.setExpiresAt(OffsetDateTime.now().plus(tanTtl));
        repo.save(ev);
        mail.sendTan(email, tan);
    }

    @Transactional
    public Optional<UUID> consumeTan(String email, String tan) {
        Optional<EmailVerificationEntity> maybe = repo
            .findFirstByEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(email);
        if (maybe.isEmpty()) return Optional.empty();
        EmailVerificationEntity ev = maybe.get();
        if (ev.getExpiresAt().isBefore(OffsetDateTime.now())) return Optional.empty();
        // Constant-time hash compare so TAN guesses can't be timing-attacked.
        byte[] storedHash = ev.getTanHash().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] providedHash = hash(tan).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (!java.security.MessageDigest.isEqual(storedHash, providedHash)) return Optional.empty();
        ev.setConsumedAt(OffsetDateTime.now());
        UUID verificationToken = UUID.randomUUID();
        ev.setVerificationToken(verificationToken);
        repo.save(ev);
        return Optional.of(verificationToken);
    }

    @Transactional
    public Optional<String> claimVerificationToken(UUID verificationToken) {
        return repo.findByVerificationToken(verificationToken).map(ev -> {
            // Consume: wipe verificationToken so it can only be used once.
            ev.setVerificationToken(null);
            repo.save(ev);
            return ev.getEmail();
        });
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String randomSixDigits() {
        SecureRandom rnd = new SecureRandom();
        int n = rnd.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
