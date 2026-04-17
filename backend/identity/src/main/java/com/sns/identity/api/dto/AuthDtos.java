package com.sns.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(@Email @NotBlank String email) {}

    public record RegisterResponse(boolean accepted) {}

    public record VerifyRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 6) String tan
    ) {}

    public record VerifyResponse(boolean verified, UUID verificationToken) {}

    public record CompleteRequest(
        @NotBlank UUID verificationToken,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String academicTitle,
        String institution,
        @NotBlank @Size(min = 8) String password
    ) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthTokens(String accessToken, String refreshToken, UUID userId) {}
}
