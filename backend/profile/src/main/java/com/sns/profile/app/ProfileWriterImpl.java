package com.sns.profile.app;

import com.sns.identity.app.ProfileWriter;
import com.sns.profile.domain.ProfileEntity;
import com.sns.profile.repo.ProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProfileWriterImpl implements ProfileWriter {

    private final ProfileRepository repo;

    public ProfileWriterImpl(ProfileRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void upsert(UUID userId, String firstName, String lastName, String academicTitle, String institution) {
        ProfileEntity p = repo.findById(userId).orElseGet(ProfileEntity::new);
        p.setUserId(userId);
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setAcademicTitle(emptyToNull(academicTitle));
        p.setInstitution(emptyToNull(institution));
        repo.save(p);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
