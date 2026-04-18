package com.sns.identity.repo;

import com.sns.identity.domain.AuditLogEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);
    List<AuditLogEntity> findByActionOrderByCreatedAtDesc(String action);

    /**
     * Single-method search powering the admin audit explorer. All filters optional. Callers must
     * substitute null timestamps with {@code OffsetDateTime.MIN}/{@code MAX} sentinels (Postgres
     * can't infer the parameter type when a bind value is plain {@code null}).
     */
    @Query("""
        SELECT a FROM AuditLogEntity a
        WHERE (:actor IS NULL OR a.actorUserId = :actor)
          AND (:action IS NULL OR a.action = :action)
          AND a.createdAt >= :since
          AND a.createdAt <= :until
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLogEntity> search(
        @Param("actor") UUID actor,
        @Param("action") String action,
        @Param("since") OffsetDateTime since,
        @Param("until") OffsetDateTime until,
        Pageable pageable
    );

    long countByCreatedAtAfter(OffsetDateTime cutoff);

    Page<AuditLogEntity> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);
}
