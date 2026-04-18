package com.sns.chat.api;

import com.sns.chat.api.dto.ChatDtos;
import com.sns.chat.app.ChatService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @GetMapping("/api/chat/{eventId}/{otherUserId}")
    public ChatDtos.HistoryResponse history(
        JwtAuthenticationToken auth,
        @PathVariable UUID eventId,
        @PathVariable UUID otherUserId,
        @RequestParam(name = "since", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since
    ) {
        var me = UUID.fromString(auth.getToken().getSubject());
        return new ChatDtos.HistoryResponse(service.history(eventId, me, otherUserId, since));
    }

    @GetMapping("/api/chats")
    public java.util.List<ChatDtos.ChatThread> threads(JwtAuthenticationToken auth) {
        var me = UUID.fromString(auth.getToken().getSubject());
        return service.listThreads(me);
    }

    @PostMapping("/api/chat/send")
    public ChatDtos.ChatMessage send(JwtAuthenticationToken auth, @jakarta.validation.Valid @RequestBody ChatDtos.SendRequest req) {
        var from = UUID.fromString(auth.getToken().getSubject());
        return service.send(from, req);
    }

    @PostMapping("/api/chat/read")
    public Map<String, Object> read(JwtAuthenticationToken auth, @jakarta.validation.Valid @RequestBody ChatDtos.MarkReadRequest req) {
        var me = UUID.fromString(auth.getToken().getSubject());
        service.markRead(me, req.messageId());
        return Map.of("ok", true);
    }
}
