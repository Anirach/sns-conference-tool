package com.sns.event.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EventDtos {

    private EventDtos() {}

    public record EventDto(
        UUID eventId,
        String eventName,
        String venue,
        String expirationCode,
        String qrCode,
        boolean expired
    ) {}

    public record JoinRequest(@NotBlank String eventCode) {}

    public record JoinResponse(EventDto event, OffsetDateTime joinedAt) {}

    public record LocationRequest(double lat, double lon, Double accuracyMeters) {}

    public record RadiusRequest(short radius) {}

    public record VicinityResponse(int radius, List<MatchDto> matches) {}

    public record MatchDto(
        UUID matchId,
        UUID eventId,
        UUID otherUserId,
        String name,
        String title,
        String institution,
        String profilePictureUrl,
        List<String> commonKeywords,
        float similarity,
        boolean mutual,
        double distanceMeters
    ) {}
}
