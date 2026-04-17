package com.sns.chat.app;

import com.sns.chat.api.dto.ChatDtos;
import com.sns.chat.domain.ChatMessageEntity;
import com.sns.chat.repo.ChatMessageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private final ChatMessageRepository repo;
    private final ApplicationEventPublisher publisher;

    public ChatService(ChatMessageRepository repo, ApplicationEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ChatMessage> history(UUID eventId, UUID me, UUID other, OffsetDateTime since) {
        return repo.findPair(eventId, me, other, since).stream()
            .map(ChatService::toDto).toList();
    }

    @Transactional
    public ChatDtos.ChatMessage send(UUID fromUserId, ChatDtos.SendRequest req) {
        if (fromUserId.equals(req.toUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot chat with self");
        }
        // Idempotent replay: if the client reused a clientMessageId, return the original row.
        if (req.clientMessageId() != null && !req.clientMessageId().isBlank()) {
            var existing = repo.findByFromUserIdAndClientMessageId(fromUserId, req.clientMessageId());
            if (existing.isPresent()) return toDto(existing.get());
        }
        var m = new ChatMessageEntity();
        m.setEventId(req.eventId());
        m.setFromUserId(fromUserId);
        m.setToUserId(req.toUserId());
        m.setContent(req.content());
        m.setClientMessageId(req.clientMessageId());
        var saved = repo.save(m);
        publisher.publishEvent(new com.sns.common.events.ChatMessageSent(
            saved.getMessageId(), saved.getEventId(), saved.getFromUserId(), saved.getToUserId(), saved.getContent(),
            saved.getCreatedAt()));
        return toDto(saved);
    }

    @Transactional
    public void markRead(UUID me, UUID messageId) {
        var m = repo.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!m.getToUserId().equals(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!m.isReadFlag()) {
            m.setReadFlag(true);
            repo.save(m);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ChatMessage> threadHeads(UUID userId) {
        return repo.findThreadHeads(userId).stream().map(ChatService::toDto).toList();
    }

    public static ChatDtos.ChatMessage toDto(ChatMessageEntity m) {
        return new ChatDtos.ChatMessage(
            m.getMessageId(),
            m.getEventId(),
            m.getFromUserId(),
            m.getToUserId(),
            m.getContent(),
            m.isReadFlag(),
            m.getCreatedAt()
        );
    }
}
