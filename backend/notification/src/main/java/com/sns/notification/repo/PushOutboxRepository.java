package com.sns.notification.repo;

import com.sns.notification.domain.PushOutboxEntity;
import com.sns.notification.domain.PushOutboxEntity.Status;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushOutboxRepository extends JpaRepository<PushOutboxEntity, UUID> {
    List<PushOutboxEntity> findByStatus(Status status, Pageable pageable);
}
