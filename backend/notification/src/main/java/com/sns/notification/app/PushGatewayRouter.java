package com.sns.notification.app;

import com.sns.notification.domain.DeviceTokenEntity;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The only {@link PushGateway} bean {@link NotificationService} sees. Since Web Push is
 * deferred (see Plan 2026-04, "Drop Flutter shell"), this router currently delegates every
 * delivery to {@link LoggingPushGateway} — outbox rows still flow through the
 * {@code @Scheduled} drain and get marked DELIVERED, but no real push fires.
 *
 * <p>When Web Push lands, replace the body of {@link #deliver} with a single call into the
 * new {@code WebPushGateway} (FCM Web v1 + VAPID).
 */
@Component
@Primary
public class PushGatewayRouter implements PushGateway {

    private final PushGateway fallback;

    public PushGatewayRouter(List<PushGateway> delegates) {
        PushGateway logging = null;
        for (PushGateway g : delegates) {
            if (g instanceof PushGatewayRouter) continue;
            if ("LoggingPushGateway".equals(g.getClass().getSimpleName())) {
                logging = g;
            }
        }
        if (logging == null) {
            throw new IllegalStateException(
                "No LoggingPushGateway registered — at least one PushGateway must always be present.");
        }
        this.fallback = logging;
    }

    @Override
    public void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception {
        fallback.deliver(device, kind, payload);
    }
}
