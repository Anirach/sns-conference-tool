package com.sns.chat.app;

import com.sns.chat.api.dto.ChatDtos;
import com.sns.chat.domain.ChatMessageEntity;
import com.sns.chat.repo.ChatMessageRepository;
import com.sns.profile.domain.ProfileEntity;
import com.sns.profile.repo.ProfileRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private final ChatMessageRepository repo;
    private final ProfileRepository profiles;
    private final ApplicationEventPublisher publisher;
    private final JdbcTemplate jdbc;

    public ChatService(
        ChatMessageRepository repo,
        ProfileRepository profiles,
        ApplicationEventPublisher publisher,
        JdbcTemplate jdbc
    ) {
        this.repo = repo;
        this.profiles = profiles;
        this.publisher = publisher;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ChatMessage> history(UUID eventId, UUID me, UUID other, OffsetDateTime since) {
        return repo.findPair(eventId, me, other, since).stream()
            .map(ChatService::toDto).toList();
    }

    /**
     * Peer profile + commonKeywords for the canonical similarity match in this event. Returns
     * {@code commonKeywords = []} when no match exists yet (e.g. two attendees with disjoint
     * interests, or before the matching sweep has run).
     */
    @Transactional(readOnly = true)
    public ChatDtos.PeerContext peerContext(UUID eventId, UUID me, UUID other) {
        ProfileEntity p = profiles.findById(other).orElse(null);
        List<String> common = lookupCommonKeywords(eventId, me, other);
        return new ChatDtos.PeerContext(
            other,
            p == null ? null : p.getFirstName(),
            p == null ? null : p.getLastName(),
            p == null ? null : p.getAcademicTitle(),
            p == null ? null : p.getInstitution(),
            p == null ? null : p.getProfilePictureUrl(),
            common
        );
    }

    private List<String> lookupCommonKeywords(UUID eventId, UUID a, UUID b) {
        if (a == null || b == null) return List.of();
        UUID lo = a.compareTo(b) < 0 ? a : b;
        UUID hi = a.compareTo(b) < 0 ? b : a;
        try {
            String[] row = jdbc.queryForObject(
                "SELECT common_keywords FROM similarity_matches "
                  + "WHERE event_id = ? AND user_id_a = ? AND user_id_b = ?",
                (rs, n) -> {
                    java.sql.Array arr = rs.getArray(1);
                    return arr == null ? new String[0] : (String[]) arr.getArray();
                },
                eventId, lo, hi
            );
            return row == null ? List.of() : Arrays.asList(row);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
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

    /**
     * Returns the inbox for {@code userId} as enriched {@link ChatDtos.ChatThread} rows — the
     * shape the participant /chats screen renders. Each row carries the other party's name +
     * institution + portrait + the thread's unread count, so the client doesn't have to fan out
     * profile lookups per thread.
     */
    @Transactional(readOnly = true)
    public List<ChatDtos.ChatThread> listThreads(UUID userId) {
        List<ChatMessageEntity> heads = repo.findThreadHeads(userId);
        if (heads.isEmpty()) return List.of();

        // One profile lookup per distinct other-user — bounded by the number of conversations
        // the participant has, which is small (tens at most).
        Map<UUID, ProfileEntity> profileByUserId = new HashMap<>();
        for (ChatMessageEntity m : heads) {
            UUID other = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
            if (!profileByUserId.containsKey(other)) {
                profiles.findById(other).ifPresent(p -> profileByUserId.put(other, p));
            }
        }

        return heads.stream()
            .map(m -> {
                UUID other = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
                ProfileEntity p = profileByUserId.get(other);
                String name = formatName(p);
                long unread = repo.countByEventIdAndFromUserIdAndToUserIdAndReadFlagFalse(
                    m.getEventId(), other, userId);
                return new ChatDtos.ChatThread(
                    "thread-" + m.getEventId() + "-" + other,
                    m.getEventId(),
                    other,
                    name,
                    p == null ? null : p.getAcademicTitle(),
                    p == null ? null : p.getInstitution(),
                    p == null ? null : p.getProfilePictureUrl(),
                    m.getContent(),
                    m.getCreatedAt(),
                    m.getFromUserId().equals(userId),
                    (int) unread
                );
            })
            .toList();
    }

    private static String formatName(ProfileEntity p) {
        if (p == null) return "Anonymous";
        StringBuilder sb = new StringBuilder();
        if (p.getFirstName() != null && !p.getFirstName().isBlank()) sb.append(p.getFirstName());
        if (p.getLastName() != null && !p.getLastName().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.getLastName());
        }
        return sb.length() == 0 ? "Anonymous" : sb.toString();
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
