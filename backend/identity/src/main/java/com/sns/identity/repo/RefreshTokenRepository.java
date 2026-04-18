package com.sns.identity.repo;

import com.sns.identity.domain.RefreshTokenEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /** Successor in the rotation chain — used by reuse-detection family revoke. */
    java.util.Optional<RefreshTokenEntity> findByReplacedBy(UUID jti);

    /** Live tokens for a user — wiped when reuse is detected to force the family out. */
    java.util.List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);
}
