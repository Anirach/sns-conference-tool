package com.sns.app.dev;

import com.sns.event.app.QrCodeService;
import com.sns.event.domain.EventEntity;
import com.sns.event.repo.EventRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds a demo event for local / CI use so the Playwright flow keeps working.
 * Disable in production: {@code sns.dev.seed-events=false}.
 */
@Configuration
public class DevSeedRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    @Bean
    @ConditionalOnProperty(name = "sns.dev.seed-events", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedDemoEvents(EventRepository events, QrCodeService qr) {
        return args -> {
            String code = "NEURIPS2026";
            String hash = qr.hash(code);
            if (events.findByQrCodeHash(hash).isPresent()) {
                log.debug("Demo event {} already present — skipping seed", code);
                return;
            }
            EventEntity e = new EventEntity();
            e.setEventName("NeurIPS 2026 Bangkok");
            e.setVenue("Queen Sirikit National Convention Center, Bangkok");
            e.setExpirationCode(OffsetDateTime.now().plusDays(30));
            e.setQrCodeHash(hash);
            e.setQrCodePlaintext(code);
            events.save(e);
            log.info("Seeded demo event {} ({})", code, e.getEventId());
        };
    }
}
