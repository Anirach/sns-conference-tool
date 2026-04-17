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
}
