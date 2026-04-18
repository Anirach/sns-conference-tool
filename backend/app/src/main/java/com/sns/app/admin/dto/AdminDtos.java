package com.sns.app.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sns.identity.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AdminDtos {

    private AdminDtos() {}

    // ---------------- pagination wrapper ----------------

    public record Page<T>(List<T> items, long total, int page, int size) {}

    // ---------------- events ----------------

    public record EventSummary(
        UUID eventId, String name, String venue, String qrCode,
        OffsetDateTime expirationCode, boolean expired, long participantCount
    ) {}

    public record EventDetail(
        UUID eventId, String name, String venue, String qrCode,
        OffsetDateTime expirationCode, boolean expired,
        Double centroidLat, Double centroidLon,
        long participantCount, long matchCount, long messageCount
    ) {}

    public record EventCreateRequest(
        @NotBlank String name,
        @NotBlank String venue,
        @NotBlank String qrCodePlaintext,
        @NotNull OffsetDateTime expirationCode,
        Double centroidLat,
        Double centroidLon
    ) {}

    public record EventUpdateRequest(
        @NotBlank String name,
        @NotBlank String venue,
        @NotNull OffsetDateTime expirationCode,
        Double centroidLat,
        Double centroidLon
    ) {}

    public record ParticipantSummary(
        UUID userId, String firstName, String lastName, String institution,
        Double lastLat, Double lastLon, OffsetDateTime lastUpdate, short selectedRadius
    ) {}

    public record HeatmapPoint(Double lat, Double lon, OffsetDateTime lastUpdate) {}

    // ---------------- users ----------------

    public record UserSummary(
        UUID userId, String email, String firstName, String lastName,
        String institution, Role role, boolean suspended, boolean deleted, OffsetDateTime createdAt
    ) {}

    public record UserDossier(
        UUID userId, String email, Role role, boolean suspended, boolean deleted,
        OffsetDateTime createdAt, OffsetDateTime suspendedAt, OffsetDateTime deletedAt,
        String firstName, String lastName, String academicTitle, String institution,
        String profilePictureUrl,
        List<InterestSummary> interests,
        List<JoinedEvent> events,
        long matchCount, long chatMessageCount, long deviceCount, long snsLinkCount,
        List<AuditEntry> recentAudit
    ) {}

    public record InterestSummary(UUID interestId, String type, String content,
                                  List<String> keywords, OffsetDateTime createdAt) {}

    public record JoinedEvent(UUID eventId, String name, OffsetDateTime joinedAt, short selectedRadius) {}

    public record RoleChangeRequest(@NotNull Role role) {}

    // ---------------- audit ----------------

    public record AuditEntry(
        UUID id, UUID actorUserId, String action, String resourceType,
        String resourceId, String payload, OffsetDateTime createdAt
    ) {}

    // ---------------- ops ----------------

    public record OutboxRow(
        UUID outboxId, UUID userId, String kind, String status,
        int attempts, String lastError, OffsetDateTime createdAt, OffsetDateTime deliveredAt
    ) {}

    public record OpsMetrics(
        UserMetrics users,
        EventMetrics events,
        OutboxMetrics outbox,
        MatchMetrics matches,
        long audit24h
    ) {
        public record UserMetrics(long total, long active, long suspended, long deleted24h) {}
        public record EventMetrics(long active, long expired) {}
        public record OutboxMetrics(long pending, long failed, long delivered24h) {}
        public record MatchMetrics(long total, long created24h) {}
    }
}
