package com.sns.notification.app;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.DeviceTokenEntity.Platform;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * FCM-backed push gateway for ANDROID and WEB device tokens. Activated only when
 * {@code sns.push.fcm.credentials-json} is set (a service-account JSON blob loaded from env).
 * <p>
 * IOS tokens are rejected (APNs is served by {@link ApnsPushGateway} in parallel).
 * {@link LoggingPushGateway} remains the fallback when neither Firebase nor APNs are configured.
 */
@Component
@ConditionalOnProperty(name = "sns.push.fcm.credentials-json")
public class FcmPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmPushGateway.class);

    private final String credentialsJson;
    private final String projectId;

    private FirebaseMessaging messaging;

    public FcmPushGateway(
        @Value("${sns.push.fcm.credentials-json}") String credentialsJson,
        @Value("${sns.push.fcm.project-id:}") String projectId
    ) {
        this.credentialsJson = credentialsJson;
        this.projectId = projectId;
    }

    @PostConstruct
    void init() throws Exception {
        try (var in = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
            var creds = GoogleCredentials.fromStream(in);
            var builder = FirebaseOptions.builder().setCredentials(creds);
            if (!projectId.isBlank()) builder.setProjectId(projectId);
            // Idempotent init: multiple beans shouldn't re-register the default app.
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(builder.build());
            }
            this.messaging = FirebaseMessaging.getInstance();
            log.info("FcmPushGateway initialised (projectId={})", projectId.isBlank() ? "from-creds" : projectId);
        }
    }

    @Override
    public void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception {
        if (device.getPlatform() == Platform.IOS) {
            // IOS goes through APNs directly; ignoring here lets the outbox attempt APNs first.
            throw new UnsupportedOperationException("IOS handled by ApnsPushGateway");
        }
        Map<String, String> data = flattenToStrings(payload);
        data.put("kind", kind);
        Message msg = Message.builder()
            .setToken(device.getToken())
            .setNotification(Notification.builder()
                .setTitle(titleFor(kind))
                .setBody(bodyFor(kind, payload))
                .build())
            .putAllData(data)
            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
            .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setContentAvailable(true).build()).build())
            .build();
        messaging.send(msg);
    }

    private static Map<String, String> flattenToStrings(Map<String, Object> in) {
        Map<String, String> out = new HashMap<>();
        for (var e : in.entrySet()) out.put(e.getKey(), String.valueOf(e.getValue()));
        return out;
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
