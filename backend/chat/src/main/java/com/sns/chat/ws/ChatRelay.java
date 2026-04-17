package com.sns.chat.ws;

import com.sns.chat.api.dto.ChatDtos;

/**
 * Cross-pod fan-out for chat messages. Implementations are picked by
 * {@code sns.chat.relay} (redis|inproc). The relay is responsible for
 * delivering a persisted message to both participants' STOMP sessions no
 * matter which pod holds them.
 */
public interface ChatRelay {
    void deliver(ChatDtos.ChatMessage message);
}
