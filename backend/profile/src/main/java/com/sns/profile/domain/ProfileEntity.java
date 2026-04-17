package com.sns.profile.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class ProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "academic_title")
    private String academicTitle;

    @Column(name = "institution")
    private String institution;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getAcademicTitle() { return academicTitle; }
    public void setAcademicTitle(String academicTitle) { this.academicTitle = academicTitle; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
