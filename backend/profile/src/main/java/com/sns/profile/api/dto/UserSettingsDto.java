package com.sns.profile.api.dto;

public record UserSettingsDto(
    boolean pushMatches,
    boolean pushChat,
    boolean gpsConsent,
    boolean keepRegister,
    String language
) {}
