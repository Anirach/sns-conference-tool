package com.sns.common.events;

import java.util.List;
import java.util.UUID;

/** Fired when a new mutual similarity match is detected. The chat-push fan-out listens. */
public record MatchFound(
    UUID matchId,
    UUID eventId,
    UUID userIdA,
    UUID userIdB,
    float similarity,
    List<String> commonKeywords
) {}
