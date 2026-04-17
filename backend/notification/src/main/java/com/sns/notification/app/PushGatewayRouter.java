package com.sns.notification.app;

import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.DeviceTokenEntity.Platform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The only {@link PushGateway} bean {@link NotificationService} sees. Routes a delivery to the
 * first backing gateway that accepts the device's platform.
 * <p>
 * Order: {@code FcmPushGateway}, {@code ApnsPushGateway}, {@code LoggingPushGateway}. The logging
 * gateway is registered unconditionally and acts as the fallback when no real gateway is
 * configured; the router only falls through to it if a platform-specific gateway declines.
 */
@Component
@Primary
public class PushGatewayRouter implements PushGateway {

    private final List<PushGateway> ordered;

    public PushGatewayRouter(List<PushGateway> delegates) {
        List<PushGateway> filtered = new ArrayList<>(delegates);
        filtered.removeIf(g -> g instanceof PushGatewayRouter);
        filtered.sort(Comparator.comparingInt(PushGatewayRouter::rank));
        this.ordered = List.copyOf(filtered);
    }

    private static int rank(PushGateway g) {
        String n = g.getClass().getSimpleName();
        if (n.equals("FcmPushGateway"))  return 0;
        if (n.equals("ApnsPushGateway")) return 1;
        return 10;
    }

    @Override
    public void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception {
        Exception lastError = null;
        for (PushGateway g : ordered) {
            if (!accepts(g, device.getPlatform())) continue;
            try {
                g.deliver(device, kind, payload);
                return;
            } catch (UnsupportedOperationException uoe) {
                // Try the next gateway.
            } catch (Exception e) {
                lastError = e;
            }
        }
        if (lastError != null) throw lastError;
    }

    private static boolean accepts(PushGateway gateway, Platform platform) {
        String n = gateway.getClass().getSimpleName();
        return switch (platform) {
            case IOS -> n.equals("ApnsPushGateway") || n.equals("LoggingPushGateway");
            case ANDROID, WEB -> n.equals("FcmPushGateway") || n.equals("LoggingPushGateway");
        };
    }
}
