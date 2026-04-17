package com.sns.common.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event fired after a chat message is persisted. The chat WS controller listens and fans
 * out to subscribers via {@code SimpMessagingTemplate}; the notification module listens and enqueues
 * a push to the recipient if they have registered device tokens.
 */
public record ChatMessageSent(
    UUID messageId,
    UUID eventId,
    UUID fromUserId,
    UUID toUserId,
    String content,
    OffsetDateTime createdAt
) {}
