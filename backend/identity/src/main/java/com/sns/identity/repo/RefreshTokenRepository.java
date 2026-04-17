package com.sns.identity.repo;

import com.sns.identity.domain.RefreshTokenEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
}
