package com.sns.notification.repo;

import com.sns.notification.domain.PushOutboxEntity;
import com.sns.notification.domain.PushOutboxEntity.Status;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushOutboxRepository extends JpaRepository<PushOutboxEntity, UUID> {

    List<PushOutboxEntity> findByStatus(Status status, Pageable pageable);

    /**
     * Selects up to {@code batch} PENDING outbox IDs with {@code FOR UPDATE SKIP LOCKED}, the
     * standard Postgres work-queue pattern. Must be called inside a @Transactional block so the
     * row locks survive until {@link #incrementAttempts(List)} lands and the caller can release
     * them before making slow gateway calls.
     */
    @Query(value = """
        SELECT outbox_id FROM push_outbox
         WHERE status = 'PENDING'
         ORDER BY created_at
         LIMIT :batch
         FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<UUID> claimPendingIds(@Param("batch") int batch);

    @Modifying
    @Query(value = "UPDATE push_outbox SET attempts = attempts + 1 WHERE outbox_id IN (:ids)",
        nativeQuery = true)
    int incrementAttempts(@Param("ids") List<UUID> ids);
}
