package com.sns.event.repo;

import com.sns.event.domain.ParticipationEntity;
import com.sns.event.domain.ParticipationId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends JpaRepository<ParticipationEntity, ParticipationId> {
    List<ParticipationEntity> findByUserId(UUID userId);
    List<ParticipationEntity> findByEventId(UUID eventId);
    void deleteByUserIdAndEventId(UUID userId, UUID eventId);
}
