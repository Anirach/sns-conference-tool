package com.sns.notification.repo;

import com.sns.notification.domain.DeviceTokenEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {
    List<DeviceTokenEntity> findByUserId(UUID userId);
    Optional<DeviceTokenEntity> findByUserIdAndToken(UUID userId, String token);
}
