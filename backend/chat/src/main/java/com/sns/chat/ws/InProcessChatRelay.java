package com.sns.chat.ws;

import com.sns.chat.api.dto.ChatDtos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Single-pod relay that bypasses Redis. Default for tests and local dev when
 * a Redis instance isn't running. Enabled by {@code sns.chat.relay=inproc}.
 */
@Component
@ConditionalOnProperty(name = "sns.chat.relay", havingValue = "inproc")
public class InProcessChatRelay implements ChatRelay {

    private final SimpMessagingTemplate template;

    public InProcessChatRelay(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void deliver(ChatDtos.ChatMessage message) {
        template.convertAndSendToUser(message.toUserId().toString(),   "/queue/chat", message);
        template.convertAndSendToUser(message.fromUserId().toString(), "/queue/chat", message);
    }
}
