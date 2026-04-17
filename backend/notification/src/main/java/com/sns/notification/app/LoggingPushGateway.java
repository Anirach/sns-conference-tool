package com.sns.notification.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.notification.domain.DeviceTokenEntity;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default gateway that emits a structured log line per attempted delivery. Registered
 * unconditionally so that {@link PushGatewayRouter} always has a fallback when neither FCM nor
 * APNs is configured for a given platform; platform-specific gateways take precedence in the
 * router's dispatch order.
 */
@Component
public class LoggingPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

    private final ObjectMapper mapper;

    public LoggingPushGateway(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception {
        log.info(
            "push.deliver kind={} user={} platform={} token={} payload={}",
            kind,
            device.getUserId(),
            device.getPlatform(),
            maskToken(device.getToken()),
            mapper.writeValueAsString(payload)
        );
    }

    private static String maskToken(String t) {
        if (t == null || t.length() <= 8) return "****";
        return t.substring(0, 4) + "****" + t.substring(t.length() - 4);
    }
}
