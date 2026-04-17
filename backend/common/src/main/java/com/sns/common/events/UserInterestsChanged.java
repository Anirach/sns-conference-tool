package com.sns.common.events;

import java.util.UUID;

/**
 * Signalled when a user's interests change (create/delete). Consumed by the matching module to
 * recompute matches for every event that user currently participates in.
 */
public record UserInterestsChanged(UUID userId) {}
