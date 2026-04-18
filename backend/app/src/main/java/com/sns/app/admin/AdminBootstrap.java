package com.sns.app.admin;

import com.sns.identity.domain.Role;
import com.sns.identity.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Promotes the user identified by {@code sns.admin.bootstrap-email} to {@link Role#SUPER_ADMIN}
 * at boot. Idempotent: running on every restart is a no-op when the role is already set.
 *
 * <p>Runs after {@code DevSeedRunner} (@Order 10) and {@code DemoDataSeeder} (@Order 20) so the
 * sentinel user exists by the time we look it up. If the email is set but the user hasn't
 * registered yet we log a warning and skip — they'll be promoted on the next boot after they
 * complete registration.
 *
 * <p>{@code ProductionSecretsCheck} refuses to start in {@code prod} when this property is unset,
 * so a production deployment cannot accidentally launch with zero admins.
 */
@Configuration
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    @Order(50)
    ApplicationRunner promoteAdminFromEnv(
        UserRepository users,
        @Value("${sns.admin.bootstrap-email:}") String email
    ) {
        return args -> {
            if (email == null || email.isBlank()) {
                log.debug("AdminBootstrap: sns.admin.bootstrap-email unset — no promotion attempted");
                return;
            }
            String normalised = email.trim().toLowerCase();
            users.findByEmailIgnoreCase(normalised).ifPresentOrElse(
                u -> {
                    if (u.getRole() == Role.SUPER_ADMIN) {
                        log.debug("AdminBootstrap: {} already SUPER_ADMIN", normalised);
                        return;
                    }
                    Role previous = u.getRole();
                    u.setRole(Role.SUPER_ADMIN);
                    users.save(u);
                    log.info("AdminBootstrap: promoted {} from {} to SUPER_ADMIN", normalised, previous);
                },
                () -> log.warn("AdminBootstrap: {} not found — will retry on next boot once they register",
                    normalised)
            );
        };
    }
}
