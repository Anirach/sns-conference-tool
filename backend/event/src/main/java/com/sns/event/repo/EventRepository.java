package com.sns.event.repo;

import com.sns.event.domain.EventEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Optional<EventEntity> findByQrCodeHash(String qrCodeHash);

    /**
     * One JOIN query that returns every event the user participates in. Replaces the
     * {@code findByUserId(participations) → loop findById(events)} N+1 pattern.
     */
    @Query("""
        SELECT e FROM EventEntity e
        WHERE e.eventId IN (
          SELECT p.eventId FROM com.sns.event.domain.ParticipationEntity p
          WHERE p.userId = :userId
        )
        """)
    List<EventEntity> findJoinedByUserId(@Param("userId") UUID userId);

    /** Substring search over name / venue / qr plaintext for the admin event list. */
    @Query("""
        SELECT e FROM EventEntity e
        WHERE LOWER(e.eventName) LIKE :q
           OR LOWER(e.venue) LIKE :q
           OR LOWER(e.qrCodePlaintext) LIKE :q
        """)
    org.springframework.data.domain.Page<EventEntity> findByQuery(
        @Param("q") String q,
        org.springframework.data.domain.Pageable pageable
    );

    long countByExpirationCodeAfter(java.time.OffsetDateTime cutoff);
    long countByExpirationCodeBefore(java.time.OffsetDateTime cutoff);
}
