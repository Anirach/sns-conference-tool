package com.sns.identity.app;

import com.sns.identity.api.dto.AuthDtos;
import com.sns.identity.domain.UserEntity;
import com.sns.identity.repo.UserRepository;
import com.sns.identity.security.RefreshTokenService;
import com.sns.identity.security.SnsJwtService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final VerificationService verification;
    private final SnsJwtService jwt;
    private final RefreshTokenService refresh;
    private final PasswordEncoder passwordEncoder;
    private final ProfileWriter profileWriter;
    private final AuditLogger audit;
    private final PasswordPolicy passwordPolicy;

    public AuthService(
        UserRepository users,
        VerificationService verification,
        SnsJwtService jwt,
        RefreshTokenService refresh,
        PasswordEncoder passwordEncoder,
        ProfileWriter profileWriter,
        AuditLogger audit,
        PasswordPolicy passwordPolicy
    ) {
        this.users = users;
        this.verification = verification;
        this.jwt = jwt;
        this.refresh = refresh;
        this.passwordEncoder = passwordEncoder;
        this.profileWriter = profileWriter;
        this.audit = audit;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional
    public void startRegistration(String email) {
        verification.startVerification(email.trim().toLowerCase());
        audit.log("auth.register", null, "user", null);
    }

    @Transactional
    public AuthDtos.VerifyResponse verifyTan(String email, String tan) {
        var tokenOpt = verification.consumeTan(email.trim().toLowerCase(), tan);
        if (tokenOpt.isEmpty()) {
            audit.log("auth.verify.failure", null);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TAN invalid or expired");
        }
        audit.log("auth.verify", null);
        return new AuthDtos.VerifyResponse(true, tokenOpt.get());
    }

    @Transactional
    public AuthDtos.AuthTokens completeRegistration(AuthDtos.CompleteRequest req) {
        String email = verification.claimVerificationToken(req.verificationToken())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token invalid"));

        passwordPolicy.validate(email, req.password());

        UserEntity user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            var u = new UserEntity();
            u.setEmail(email);
            return users.save(u);
        });
        user.setEmailVerified(true);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(user);

        profileWriter.upsert(user.getUserId(), req.firstName(), req.lastName(), req.academicTitle(), req.institution());

        audit.log("auth.complete", user.getUserId(), "user", user.getUserId().toString());
        return issueTokens(user.getUserId());
    }

    /**
     * BCrypt hash of the literal string {@code "$phantom-password$"}, computed once at class load
     * with the same cost factor as the production encoder (12). When a login arrives for an
     * unknown email we still run {@link PasswordEncoder#matches(CharSequence, String)} against
     * this hash so the response timing matches the bad-password path exactly. Without this,
     * "user does not exist" returns roughly 200 ms faster than "password mismatch", which is
     * enough to enumerate accounts.
     */
    private static final String PHANTOM_HASH =
        new BCryptPasswordEncoder(12).encode("$phantom-password$");

    @Transactional
    public AuthDtos.AuthTokens login(String email, String password) {
        Optional<UserEntity> maybe = users.findByEmailIgnoreCase(email.trim().toLowerCase());
        if (maybe.isEmpty()) {
            // Burn the same wall-clock as a real BCrypt compare so timing doesn't leak account
            // existence; deliberately ignore the result.
            passwordEncoder.matches(password, PHANTOM_HASH);
            audit.log("auth.login.failure", null);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        UserEntity user = maybe.get();
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            audit.log("auth.login.failure", user.getUserId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            audit.log("auth.login.unverified", user.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email not verified");
        }
        audit.log("auth.login", user.getUserId());
        return issueTokens(user.getUserId());
    }

    @Transactional
    public AuthDtos.AuthTokens refresh(String presentedJti) {
        UUID jti = parseUuid(presentedJti);
        try {
            var next = refresh.rotate(jti);
            audit.log("auth.refresh", next.getUserId());
            return new AuthDtos.AuthTokens(jwt.issueAccessToken(next.getUserId()), next.getJti().toString(), next.getUserId());
        } catch (RefreshTokenService.InvalidRefreshTokenException e) {
            if ("token reused".equals(e.getMessage())) {
                audit.log("auth.refresh.reuse_detected", null, "refresh_token", jti.toString());
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    @Transactional
    public void logout(String presentedJti) {
        try {
            UUID jti = UUID.fromString(presentedJti);
            refresh.revoke(jti);
            audit.log("auth.logout", null, "refresh_token", jti.toString());
        } catch (IllegalArgumentException ignored) {
            // Idempotent: logging out with a malformed token is a no-op.
        }
    }

    private AuthDtos.AuthTokens issueTokens(UUID userId) {
        var access = jwt.issueAccessToken(userId);
        var rt = refresh.issue(userId);
        return new AuthDtos.AuthTokens(access, rt.getJti().toString(), userId);
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }
}
