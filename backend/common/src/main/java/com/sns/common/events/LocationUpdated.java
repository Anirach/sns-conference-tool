package com.sns.common.events;

import java.util.UUID;

/** Fired after a participant's GPS fix is persisted. Used to invalidate vicinity caches. */
public record LocationUpdated(UUID eventId, UUID userId) {}
