package com.sns.profile.api.dto;

/**
 * Each field is optional ({@code null} means "leave this preference alone"). A full GET → tweak →
 * PUT round-trip is the canonical update path, but partial updates are accepted for forward
 * compatibility with future toggles that older clients won't know about.
 */
public record UpdateUserSettingsRequest(
    Boolean pushMatches,
    Boolean pushChat,
    Boolean gpsConsent,
    Boolean keepRegister,
    String language
) {}
