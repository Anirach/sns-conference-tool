package com.sns.common.events;

import java.util.UUID;

/**
 * Published whenever something changes that could invalidate pre-computed matches for an event
 * (join, leave, new interest, deleted interest). The matching module listens and re-runs the
 * similarity computation for that event.
 * <p>
 * Publishing side should not block on recompute — the listener runs async where possible.
 */
public record MatchRecomputeRequested(UUID eventId) {}
