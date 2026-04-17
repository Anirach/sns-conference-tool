package com.sns.profile.api.dto;

public record UpdateProfileRequest(
    String firstName,
    String lastName,
    String academicTitle,
    String institution,
    String profilePictureUrl
) {}
