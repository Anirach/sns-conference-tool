package com.sns.notification.app;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.DeviceTokenEntity.Platform;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pushy-backed APNs gateway for IOS device tokens. Activated only when {@code sns.push.apns.team-id}
 * is set. Uses token-based auth (JWT-signed over a .p8 private key) rather than certificate pinning.
 * <p>
 * ANDROID / WEB tokens are rejected — those route to {@link FcmPushGateway}.
 */
@Component
@ConditionalOnProperty(name = "sns.push.apns.team-id")
public class ApnsPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(ApnsPushGateway.class);

    private final String teamId;
    private final String keyId;
    private final String bundleId;
    private final String signingKeyPem;
    private final boolean sandbox;

    private ApnsClient client;

    public ApnsPushGateway(
        @Value("${sns.push.apns.team-id}") String teamId,
        @Value("${sns.push.apns.key-id}") String keyId,
        @Value("${sns.push.apns.bundle-id}") String bundleId,
        @Value("${sns.push.apns.signing-key-pem}") String signingKeyPem,
        @Value("${sns.push.apns.sandbox:false}") boolean sandbox
    ) {
        this.teamId = teamId;
        this.keyId = keyId;
        this.bundleId = bundleId;
        this.signingKeyPem = signingKeyPem;
        this.sandbox = sandbox;
    }

    @PostConstruct
    void init() throws Exception {
        try (var in = new ByteArrayInputStream(signingKeyPem.getBytes(StandardCharsets.UTF_8))) {
            var signingKey = ApnsSigningKey.loadFromInputStream(in, teamId, keyId);
            this.client = new ApnsClientBuilder()
                .setApnsServer(sandbox
                    ? ApnsClientBuilder.DEVELOPMENT_APNS_HOST
                    : ApnsClientBuilder.PRODUCTION_APNS_HOST)
                .setSigningKey(signingKey)
                .build();
        }
        log.info("ApnsPushGateway initialised (team={}, sandbox={})", teamId, sandbox);
    }

    @PreDestroy
    void shutdown() {
        if (client != null) client.close();
    }

    @Override
    public void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception {
        if (device.getPlatform() != Platform.IOS) {
            throw new UnsupportedOperationException("ApnsPushGateway only handles IOS");
        }
        String body = new SimpleApnsPayloadBuilder()
            .setAlertTitle(titleFor(kind))
            .setAlertBody(bodyFor(kind, payload))
            .addCustomProperty("kind", kind)
            .addCustomProperty("data", payload)
            .build();
        SimpleApnsPushNotification note = new SimpleApnsPushNotification(
            TokenUtil.sanitizeTokenString(device.getToken()),
            bundleId,
            body
        );
        var response = client.sendNotification(note).get();
        if (!response.isAccepted()) {
            throw new RuntimeException("APNs rejected: " + response.getRejectionReason());
        }
    }

    private static String titleFor(String kind) {
        return switch (kind) {
            case "match.found" -> "A new fellow nearby";
            case "chat.message" -> "New message";
            default -> "SNS";
        };
    }

    private static String bodyFor(String kind, Map<String, Object> payload) {
        if ("chat.message".equals(kind) && payload.get("preview") instanceof String p) return p;
        if ("match.found".equals(kind)) return "Open the app to see who shares your interests.";
        return "";
    }
}
