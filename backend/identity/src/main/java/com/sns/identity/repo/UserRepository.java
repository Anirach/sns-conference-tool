package com.sns.identity.repo;

import com.sns.identity.domain.Role;
import com.sns.identity.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Hard-delete candidates: soft-deleted before {@code cutoff}. Replaces full-table scans in
     * {@link com.sns.app.gdpr.HardDeleteJob} that load every user via {@code findAll()}.
     */
    java.util.List<UserEntity> findByDeletedAtIsNotNullAndDeletedAtBefore(
        java.time.OffsetDateTime cutoff,
        Pageable page
    );

    /**
     * Admin user search. All filters optional ({@code IS NULL OR …}). {@code statusFilter}:
     * {@code null} = any; {@code "active"} = not deleted, not suspended;
     * {@code "suspended"} = not deleted, suspended; {@code "deleted"} = soft-deleted.
     */
    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:q IS NULL OR LOWER(CAST(u.email AS string)) LIKE :q)
          AND (:role IS NULL OR u.role = :role)
          AND (
                :statusFilter IS NULL
             OR (:statusFilter = 'active'    AND u.deletedAt IS NULL AND u.suspendedAt IS NULL)
             OR (:statusFilter = 'suspended' AND u.deletedAt IS NULL AND u.suspendedAt IS NOT NULL)
             OR (:statusFilter = 'deleted'   AND u.deletedAt IS NOT NULL)
          )
        ORDER BY u.createdAt DESC
        """)
    Page<UserEntity> searchAdmin(
        @Param("q") String q,
        @Param("role") Role role,
        @Param("statusFilter") String statusFilter,
        Pageable pageable
    );

    long countByDeletedAtIsNullAndSuspendedAtIsNull();
    long countBySuspendedAtIsNotNullAndDeletedAtIsNull();
    long countByDeletedAtAfter(java.time.OffsetDateTime cutoff);
    long countByRole(Role role);
}
