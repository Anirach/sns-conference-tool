package com.sns.interest.repo;

import com.sns.interest.domain.InterestEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<InterestEntity, UUID> {
    List<InterestEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long deleteByInterestIdAndUserId(UUID interestId, UUID userId);
}
