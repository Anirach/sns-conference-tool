package com.sns.chat.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ChatDtos {

    private ChatDtos() {}

    public record ChatMessage(
        UUID messageId,
        UUID eventId,
        UUID fromUserId,
        UUID toUserId,
        String content,
        boolean readFlag,
        OffsetDateTime createdAt
    ) {}

    public record HistoryResponse(List<ChatMessage> messages) {}

    public record SendRequest(
        @NotNull UUID eventId,
        @NotNull UUID toUserId,
        @NotBlank @Size(max = 4000) String content,
        // Client-supplied idempotency key so reconnects can dedupe.
        String clientMessageId
    ) {}

    public record MarkReadRequest(
        @NotNull UUID eventId,
        @NotNull UUID messageId
    ) {}

    public record ChatThread(
        String threadId,
        UUID eventId,
        UUID otherUserId,
        String otherName,
        String otherTitle,
        String otherInstitution,
        String otherPictureUrl,
        String lastMessagePreview,
        OffsetDateTime lastMessageAt,
        boolean lastFromMe,
        int unread
    ) {}
}
