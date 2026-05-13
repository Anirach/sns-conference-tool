package com.sns.profile.repo;

import com.sns.profile.domain.UserSettingsEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, UUID> {
}
