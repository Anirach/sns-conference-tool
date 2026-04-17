package com.sns.event.repo;

import com.sns.event.domain.EventEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {
    Optional<EventEntity> findByQrCodeHash(String qrCodeHash);
}
