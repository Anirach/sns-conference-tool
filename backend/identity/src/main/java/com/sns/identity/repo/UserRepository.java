package com.sns.identity.repo;

import com.sns.identity.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Hard-delete candidates: soft-deleted before {@code cutoff}. Replaces full-table scans in
     * {@link com.sns.app.gdpr.HardDeleteJob} that load every user via {@code findAll()}.
     */
    java.util.List<UserEntity> findByDeletedAtIsNotNullAndDeletedAtBefore(
        java.time.OffsetDateTime cutoff,
        org.springframework.data.domain.Pageable page
    );
}
