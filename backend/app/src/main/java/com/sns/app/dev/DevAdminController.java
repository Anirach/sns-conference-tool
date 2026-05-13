package com.sns.app.dev;

import com.sns.identity.app.AuditLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Dev-only admin endpoints. Bean registration is conditional on {@link DemoDataSeeder} being
 * present, which itself is gated on {@code sns.dev.seed-demo-data=true}. Also profile-gated so
 * it never spins up under {@code prod}. Restricted to SUPER_ADMIN so a stray ADMIN can't wipe
 * the demo state mid-conference.
 */
@RestController
@RequestMapping("/api/admin/dev")
@Profile("!prod")
@ConditionalOnProperty(name = "sns.dev.seed-demo-data", havingValue = "true")
public class DevAdminController {

    private final DemoDataSeeder seeder;
    private final AuditLogger audit;

    public DevAdminController(DemoDataSeeder seeder, AuditLogger audit) {
        this.seeder = seeder;
        this.audit = audit;
    }

    @PostMapping("/reset-demo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void resetDemo(JwtAuthenticationToken auth) {
        UUID actor = currentUserId(auth);
        audit.log("admin.dev.reset_demo", actor, "demo_data", "all");
        seeder.reset();
    }

    private static UUID currentUserId(JwtAuthenticationToken auth) {
        Jwt token = auth.getToken();
        try {
            return UUID.fromString(token.getSubject());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject");
        }
    }
}
