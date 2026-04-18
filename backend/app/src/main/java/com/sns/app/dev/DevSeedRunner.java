package com.sns.app.dev;

import com.sns.event.app.QrCodeService;
import com.sns.event.domain.EventEntity;
import com.sns.event.repo.EventRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Seeds the three demo events the MSW mocks ship — NeurIPS Bangkok (active), ACL Vienna (active),
 * ICML Montreal (already expired). Idempotent: each event is keyed by its QR code hash so re-runs
 * are no-ops. Disable in production: {@code sns.dev.seed-events=false}.
 */
@Configuration
public class DevSeedRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private record SeedEvent(String code, String name, String venue, long daysFromNow) {}

    private static final List<SeedEvent> EVENTS = List.of(
        new SeedEvent("NEURIPS2026", "NeurIPS 2026 Bangkok",
            "Queen Sirikit National Convention Center, Bangkok", 3),
        new SeedEvent("ACL2026", "ACL 2026 Vienna",
            "Austria Center Vienna", 5),
        new SeedEvent("ICML2025", "ICML 2025 Montreal",
            "Palais des Congrès de Montréal", -30)
    );

    @Bean
    @Order(10)
    @ConditionalOnProperty(name = "sns.dev.seed-events", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedDemoEvents(EventRepository events, QrCodeService qr) {
        return args -> {
            for (SeedEvent s : EVENTS) {
                String hash = qr.hash(s.code());
                if (events.findByQrCodeHash(hash).isPresent()) {
                    log.debug("Demo event {} already present — skipping", s.code());
                    continue;
                }
                EventEntity e = new EventEntity();
                e.setEventName(s.name());
                e.setVenue(s.venue());
                e.setExpirationCode(OffsetDateTime.now().plusDays(s.daysFromNow()));
                e.setQrCodeHash(hash);
                e.setQrCodePlaintext(s.code());
                events.save(e);
                log.info("Seeded demo event {} ({})", s.code(), e.getEventId());
            }
        };
    }
}
