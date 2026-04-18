package com.sns.sns.repo;

import com.sns.sns.domain.SnsLinkEntity;
import com.sns.sns.domain.SnsLinkEntity.Provider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnsLinkRepository extends JpaRepository<SnsLinkEntity, UUID> {
    List<SnsLinkEntity> findByUserId(UUID userId);
    Optional<SnsLinkEntity> findByUserIdAndProvider(UUID userId, Provider provider);

    /** Stale-link slice for {@code SnsEnrichmentJob}. Replaces a full-table scan + filter. */
    List<SnsLinkEntity> findByLastFetchIsNullOrLastFetchBefore(
        java.time.OffsetDateTime cutoff,
        org.springframework.data.domain.Pageable page
    );
}
