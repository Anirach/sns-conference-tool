package com.sns.common.events;

import java.util.UUID;

/**
 * Fired when a single user joins an event. Triggers an incremental similarity recompute
 * involving only pairs ({@code userId}, *) — O(N) instead of the full O(N²) sweep.
 */
public record UserJoinedEvent(UUID eventId, UUID userId) {}
