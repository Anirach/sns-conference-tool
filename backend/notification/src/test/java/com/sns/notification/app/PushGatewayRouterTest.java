package com.sns.notification.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sns.notification.domain.DeviceTokenEntity;
import com.sns.notification.domain.DeviceTokenEntity.Platform;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * After dropping the Flutter shell, every push subscription comes from the web app and
 * Web Push is deferred — so {@link PushGatewayRouter} delegates everything to the
 * logging gateway. These tests pin that contract: the router needs the logging delegate,
 * routes any platform to it, and refuses to start without it.
 */
class PushGatewayRouterTest {

    @Test
    void routesEverythingToLoggingGateway() throws Exception {
        var logging = new LoggingPushGateway();
        var router = new PushGatewayRouter(List.of(logging));
        router.deliver(device(Platform.WEB), "match.found", Map.of());
        router.deliver(device(Platform.ANDROID), "chat.message", Map.of());
        router.deliver(device(Platform.IOS), "match.found", Map.of());
        assertThat(logging.calls.get()).isEqualTo(3);
    }

    @Test
    void requiresLoggingGateway() {
        assertThatThrownBy(() -> new PushGatewayRouter(List.of()))
            .isInstanceOf(IllegalStateException.class);
    }

    static class LoggingPushGateway implements PushGateway {
        final AtomicInteger calls = new AtomicInteger();
        @Override public void deliver(DeviceTokenEntity d, String k, Map<String, Object> p) {
            calls.incrementAndGet();
        }
    }

    private static DeviceTokenEntity device(Platform platform) {
        var d = new DeviceTokenEntity();
        d.setUserId(UUID.randomUUID());
        d.setPlatform(platform);
        d.setToken("tok-" + platform);
        return d;
    }
}
