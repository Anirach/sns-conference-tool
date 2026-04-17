package com.sns.interest.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sns.interest.domain.InterestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class InterestDtos {

    private InterestDtos() {}

    public record CreateRequest(
        @NotNull InterestType type,
        @NotBlank String content,
        String articleUrl
    ) {}

    public record InterestDto(
        UUID interestId,
        UUID userId,
        InterestType type,
        String content,
        List<String> extractedKeywords,
        OffsetDateTime createdAt
    ) {}
}
