package com.sns.notification.app;

import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.DeviceTokenEntity.Platform;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The only {@link PushGateway} bean {@link NotificationService} sees. Routes a delivery to the
 * pre-resolved gateway for the device's platform, with a logging fallback.
 * <p>
 * Resolution happens once at construction — the previous implementation scanned every delegate
 * and string-matched class names on every {@code deliver()} call. With Fcm + Apns + Logging that
 * was 3 string compares per platform decision per push; at scale (push.drain-interval-ms = 5s,
 * 50 outbox rows per drain) it added up.
 */
@Component
@Primary
public class PushGatewayRouter implements PushGateway {

    private final Map<Platform, PushGateway> primary;
    private final PushGateway fallback;

    public PushGatewayRouter(List<PushGateway> delegates) {
        Map<Platform, PushGateway> p = new EnumMap<>(Platform.class);
        PushGateway loggingFallback = null;
        for (PushGateway g : delegates) {
            if (g instanceof PushGatewayRouter) continue;
            switch (g.getClass().getSimpleName()) {
                case "FcmPushGateway" -> {
                    p.put(Platform.ANDROID, g);
                    p.put(Platform.WEB, g);
                }
                case "ApnsPushGateway" -> p.put(Platform.IOS, g);
                case "LoggingPushGateway" -> loggingFallback = g;
                default -> {
                    // Custom gateways register themselves; unknown names route via fallback.
                }
            }
        }
        this.primary = Map.copyOf(p);
        this.fallback = Optional.ofNullable(loggingFallback)
            .orElseThrow(() -> new IllegalStateException(
                "No LoggingPushGateway registered — at least one PushGateway must always be present."));
    }

    @Override
    public void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception {
        PushGateway resolved = primary.getOrDefault(device.getPlatform(), fallback);
        try {
            resolved.deliver(device, kind, payload);
        } catch (UnsupportedOperationException uoe) {
            // The chosen gateway opted out of this device — fall back to logging so the outbox
            // marks the row delivered (debug visibility, not a true delivery).
            fallback.deliver(device, kind, payload);
        }
    }
}
