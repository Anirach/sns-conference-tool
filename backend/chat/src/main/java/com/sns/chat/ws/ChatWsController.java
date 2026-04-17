package com.sns.chat.ws;

import com.sns.chat.api.dto.ChatDtos;
import com.sns.chat.app.ChatService;
import com.sns.common.events.ChatMessageSent;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

/**
 * STOMP controllers for chat.
 * <ul>
 *   <li>{@code /app/chat.send}       — persist + publish {@link ChatMessageSent}</li>
 *   <li>{@code /app/chat.markRead}   — mark a message read; echoes back to the sender</li>
 *   <li>{@code /user/queue/chat}     — inbound messages per user (server → client)</li>
 * </ul>
 */
@Controller
public class ChatWsController {

    private final ChatService service;
    private final SimpMessagingTemplate template;

    public ChatWsController(ChatService service, SimpMessagingTemplate template) {
        this.service = service;
        this.template = template;
    }

    @MessageMapping("/chat.send")
    public void send(JwtAuthenticationToken auth, ChatDtos.SendRequest req) {
        UUID from = UUID.fromString(auth.getToken().getSubject());
        service.send(from, req);
        // Persisted-and-fan-out is driven by ChatMessageSent domain event (below).
    }

    @MessageMapping("/chat.markRead")
    @SendToUser("/queue/chat.readReceipts")
    public ChatDtos.MarkReadRequest markRead(JwtAuthenticationToken auth, ChatDtos.MarkReadRequest req) {
        UUID me = UUID.fromString(auth.getToken().getSubject());
        service.markRead(me, req.messageId());
        return req;
    }

    /** Server-initiated fan-out after persistence. */
    @EventListener
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
        // Deliver to both ends so sender sees its own echo (e.g. on a second device).
        template.convertAndSendToUser(event.toUserId().toString(),   "/queue/chat", dto);
        template.convertAndSendToUser(event.fromUserId().toString(), "/queue/chat", dto);
    }
}
