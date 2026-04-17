package com.sns.identity.repo;

import com.sns.identity.domain.EmailVerificationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, UUID> {
    Optional<EmailVerificationEntity> findFirstByEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(String email);
    Optional<EmailVerificationEntity> findByVerificationToken(UUID verificationToken);
}
