package com.sns.notification.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.notification.domain.DeviceTokenEntity;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default gateway used until a real FCM / APNs integration is configured. Emits a single
 * structured log line per attempted delivery so pipelines can count delivery attempts without
 * any external dependency.
 */
@Configuration
public class LoggingPushGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

    @Bean
    @ConditionalOnMissingBean(PushGateway.class)
    PushGateway loggingGateway(ObjectMapper mapper) {
        return (device, kind, payload) -> {
            log.info(
                "push.deliver kind={} user={} platform={} token={} payload={}",
                kind,
                device.getUserId(),
                device.getPlatform(),
                maskToken(device.getToken()),
                mapper.writeValueAsString(payload)
            );
        };
    }

    private static String maskToken(String t) {
        if (t == null || t.length() <= 8) return "****";
        return t.substring(0, 4) + "****" + t.substring(t.length() - 4);
    }
}
