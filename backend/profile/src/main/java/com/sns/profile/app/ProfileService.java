package com.sns.profile.app;

import com.sns.identity.domain.UserEntity;
import com.sns.identity.repo.UserRepository;
import com.sns.profile.api.dto.UpdateProfileRequest;
import com.sns.profile.api.dto.UserDto;
import com.sns.profile.domain.ProfileEntity;
import com.sns.profile.repo.ProfileRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final UserRepository users;

    public ProfileService(ProfileRepository profiles, UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID userId) {
        UserEntity user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ProfileEntity profile = profiles.findById(userId).orElseGet(() -> {
            var p = new ProfileEntity();
            p.setUserId(userId);
            return p;
        });
        return toDto(user, profile);
    }

    @Transactional
    public UserDto update(UUID userId, UpdateProfileRequest req) {
        ProfileEntity profile = profiles.findById(userId).orElseGet(() -> {
            var p = new ProfileEntity();
            p.setUserId(userId);
            return p;
        });
        if (req.firstName() != null)          profile.setFirstName(req.firstName());
        if (req.lastName() != null)           profile.setLastName(req.lastName());
        if (req.academicTitle() != null)      profile.setAcademicTitle(blankToNull(req.academicTitle()));
        if (req.institution() != null)        profile.setInstitution(blankToNull(req.institution()));
        if (req.profilePictureUrl() != null)  profile.setProfilePictureUrl(blankToNull(req.profilePictureUrl()));
        profiles.save(profile);

        UserEntity user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toDto(user, profile);
    }

    private static UserDto toDto(UserEntity user, ProfileEntity p) {
        return new UserDto(
            user.getUserId(),
            user.getEmail(),
            p.getFirstName(),
            p.getLastName(),
            p.getAcademicTitle(),
            p.getInstitution(),
            p.getProfilePictureUrl()
        );
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
