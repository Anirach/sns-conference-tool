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
 * PushGatewayRouter dispatches by the simple class name of each injected delegate — so these
 * test spies are named {@code FcmPushGateway}, {@code ApnsPushGateway}, {@code LoggingPushGateway}
 * to match the runtime match keys.
 */
class PushGatewayRouterTest {

    @Test
    void iosRoutesToApnsWhenAvailable() throws Exception {
        var apns = new ApnsPushGateway();
        var logging = new LoggingPushGateway();
        var router = new PushGatewayRouter(List.of(apns, logging));
        router.deliver(device(Platform.IOS), "match.found", Map.of());
        assertThat(apns.calls.get()).isEqualTo(1);
        assertThat(logging.calls.get()).isEqualTo(0);
    }

    @Test
    void androidRoutesToFcmWhenAvailable() throws Exception {
        var fcm = new FcmPushGateway();
        var logging = new LoggingPushGateway();
        var router = new PushGatewayRouter(List.of(fcm, logging));
        router.deliver(device(Platform.ANDROID), "match.found", Map.of());
        assertThat(fcm.calls.get()).isEqualTo(1);
    }

    @Test
    void webRoutesToFcmWhenAvailable() throws Exception {
        var fcm = new FcmPushGateway();
        var logging = new LoggingPushGateway();
        var router = new PushGatewayRouter(List.of(fcm, logging));
        router.deliver(device(Platform.WEB), "match.found", Map.of());
        assertThat(fcm.calls.get()).isEqualTo(1);
    }

    @Test
    void fallsBackToLoggingWhenPrimaryRejects() throws Exception {
        var apns = new ApnsPushGateway();
        apns.rejecting = true;
        var logging = new LoggingPushGateway();
        var router = new PushGatewayRouter(List.of(apns, logging));
        router.deliver(device(Platform.IOS), "x", Map.of());
        assertThat(logging.calls.get()).isEqualTo(1);
    }

    @Test
    void requiresAtLeastOneLoggingGateway() {
        assertThatThrownBy(() -> new PushGatewayRouter(List.of(new FcmPushGateway())))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void androidWithoutFcmFallsBackToLogging() throws Exception {
        var logging = new LoggingPushGateway();
        var router = new PushGatewayRouter(List.of(logging));
        router.deliver(device(Platform.ANDROID), "x", Map.of());
        assertThat(logging.calls.get()).isEqualTo(1);
    }

    // ── Named test doubles (simpleName must match the runtime dispatcher) ──

    static class FcmPushGateway implements PushGateway {
        final AtomicInteger calls = new AtomicInteger();
        @Override public void deliver(DeviceTokenEntity d, String k, Map<String, Object> p) {
            calls.incrementAndGet();
        }
    }

    static class ApnsPushGateway implements PushGateway {
        final AtomicInteger calls = new AtomicInteger();
        boolean rejecting = false;
        @Override public void deliver(DeviceTokenEntity d, String k, Map<String, Object> p) {
            if (rejecting) throw new UnsupportedOperationException("wrong platform");
            calls.incrementAndGet();
        }
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
