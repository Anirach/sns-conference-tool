package com.sns.identity.repo;

import com.sns.identity.domain.AuditLogEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);
    List<AuditLogEntity> findByActionOrderByCreatedAtDesc(String action);
}
