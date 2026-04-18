package com.sns.chat.repo;

import com.sns.chat.domain.ChatMessageEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    Optional<ChatMessageEntity> findByFromUserIdAndClientMessageId(UUID fromUserId, String clientMessageId);

    /** All messages where the user is sender or recipient — used by the GDPR export aggregator. */
    List<ChatMessageEntity> findByFromUserIdOrToUserIdOrderByCreatedAtAsc(UUID fromUserId, UUID toUserId);

    long countByEventId(UUID eventId);

    long countByFromUserIdOrToUserId(UUID fromUserId, UUID toUserId);

    @Query("""
        SELECT m FROM ChatMessageEntity m
        WHERE m.eventId = :eventId
          AND ((m.fromUserId = :u1 AND m.toUserId = :u2)
            OR (m.fromUserId = :u2 AND m.toUserId = :u1))
          AND (:since IS NULL OR m.createdAt > :since)
        ORDER BY m.createdAt ASC
        """)
    List<ChatMessageEntity> findPair(
        @Param("eventId") UUID eventId,
        @Param("u1") UUID u1,
        @Param("u2") UUID u2,
        @Param("since") OffsetDateTime since
    );

    @Query("""
        SELECT m FROM ChatMessageEntity m
        WHERE (m.fromUserId = :userId OR m.toUserId = :userId)
          AND m.createdAt = (
            SELECT MAX(m2.createdAt) FROM ChatMessageEntity m2
            WHERE m2.eventId = m.eventId
              AND ((m2.fromUserId = :userId AND m2.toUserId = CASE WHEN m.fromUserId = :userId THEN m.toUserId ELSE m.fromUserId END)
                OR (m2.toUserId = :userId AND m2.fromUserId = CASE WHEN m.fromUserId = :userId THEN m.toUserId ELSE m.fromUserId END))
          )
        ORDER BY m.createdAt DESC
        """)
    List<ChatMessageEntity> findThreadHeads(@Param("userId") UUID userId);
}
