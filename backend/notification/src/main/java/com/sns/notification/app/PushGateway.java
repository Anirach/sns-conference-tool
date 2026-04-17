package com.sns.notification.app;

import com.sns.notification.domain.DeviceTokenEntity;
import java.util.Map;

/**
 * Abstraction over FCM / APNs. A real implementation would wrap the Firebase Admin SDK and the
 * APNs HTTP/2 client. The default {@link LoggingPushGateway} just emits structured log lines — fine
 * for dev and for tests. Swap in a cloud-backed implementation via configuration.
 */
public interface PushGateway {

    /**
     * Delivers a payload to a single device. Returning normally means delivery was ACK'd; a thrown
     * exception triggers retry + eventual {@code FAILED} status on the outbox row.
     */
    void deliver(DeviceTokenEntity device, String kind, Map<String, Object> payload) throws Exception;
}
