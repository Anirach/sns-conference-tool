package com.sns.matching.repo;

import com.sns.matching.domain.SimilarityMatchEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimilarityMatchRepository extends JpaRepository<SimilarityMatchEntity, UUID> {
    Optional<SimilarityMatchEntity> findByEventIdAndUserIdAAndUserIdB(UUID eventId, UUID userIdA, UUID userIdB);
}
