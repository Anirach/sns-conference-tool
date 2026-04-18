package com.sns.chat.ws;

import com.sns.chat.api.dto.ChatDtos;
import com.sns.chat.app.ChatService;
import com.sns.common.events.ChatMessageSent;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * STOMP controllers for chat.
 * <ul>
 *   <li>{@code /app/chat.send}       — persist + publish {@link ChatMessageSent}</li>
 *   <li>{@code /app/chat.markRead}   — mark a message read; echoes back to the sender</li>
 *   <li>{@code /user/queue/chat}     — inbound messages per user (server → client, delivered via ChatRelay)</li>
 * </ul>
 */
@Controller
public class ChatWsController {

    private final ChatService service;
    private final ChatRelay relay;

    public ChatWsController(ChatService service, ChatRelay relay) {
        this.service = service;
        this.relay = relay;
    }

    @MessageMapping("/chat.send")
    public void send(JwtAuthenticationToken auth, @jakarta.validation.Valid ChatDtos.SendRequest req) {
        UUID from = UUID.fromString(auth.getToken().getSubject());
        service.send(from, req);
        // Fan-out happens in onMessageSent after the persist transaction commits.
    }

    @MessageMapping("/chat.markRead")
    @SendToUser("/queue/chat.readReceipts")
    public ChatDtos.MarkReadRequest markRead(JwtAuthenticationToken auth, @jakarta.validation.Valid ChatDtos.MarkReadRequest req) {
        UUID me = UUID.fromString(auth.getToken().getSubject());
        service.markRead(me, req.messageId());
        return req;
    }

    /**
     * Server-initiated fan-out once the persist transaction commits. The injected ChatRelay is
     * either InProcessChatRelay (tests / single pod) or RedisChatRelay (default, multi-pod).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(ChatMessageSent event) {
        var dto = new ChatDtos.ChatMessage(
            event.messageId(),
            event.eventId(),
            event.fromUserId(),
            event.toUserId(),
            event.content(),
            false,
            event.createdAt()
        );
        relay.deliver(dto);
    }
}
