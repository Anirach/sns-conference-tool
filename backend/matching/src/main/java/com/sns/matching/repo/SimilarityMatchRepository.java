package com.sns.matching.repo;

import com.sns.matching.domain.SimilarityMatchEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimilarityMatchRepository extends JpaRepository<SimilarityMatchEntity, UUID> {
    Optional<SimilarityMatchEntity> findByEventIdAndUserIdAAndUserIdB(UUID eventId, UUID userIdA, UUID userIdB);

    /** Matches involving a specific user, used by the GDPR export aggregator. */
    java.util.List<SimilarityMatchEntity> findByUserIdAOrUserIdB(UUID userIdA, UUID userIdB);

    /** All matches scoped to a single event — used by the admin event detail page. */
    java.util.List<SimilarityMatchEntity> findByEventId(UUID eventId);

    long countByCreatedAtAfter(java.time.OffsetDateTime cutoff);
}
