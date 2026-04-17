package com.sns.profile.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record UserDto(
    UUID userId,
    String email,
    String firstName,
    String lastName,
    String academicTitle,
    String institution,
    String profilePictureUrl
) {}
