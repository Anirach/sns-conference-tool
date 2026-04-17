package com.sns.profile.repo;

import com.sns.profile.domain.ProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {
}
