package com.sns.matching.app;

import com.sns.common.events.MatchRecomputeRequested;
import com.sns.common.events.UserInterestsChanged;
import com.sns.common.events.UserJoinedEvent;
import com.sns.event.repo.ParticipationRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MatchingEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchingEventListener.class);

    private final MatchingService matching;
    private final ParticipationRepository participations;

    public MatchingEventListener(MatchingService matching, ParticipationRepository participations) {
        this.matching = matching;
        this.participations = participations;
    }

    /**
     * Coarse trigger — used by event leave (and as a manual recompute hook). Cost is O(N²).
     * Do NOT publish this on single-user changes; use {@link UserJoinedEvent} or
     * {@link UserInterestsChanged} instead so we hit the O(N) incremental path.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onParticipationChanged(MatchRecomputeRequested event) {
        try {
            matching.recompute(event.eventId());
        } catch (Exception e) {
            log.warn("Matching recompute failed for event {}: {}", event.eventId(), e.toString());
        }
    }

    /** Single-user join → only recompute pairs (joining-user, *). O(N). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserJoined(UserJoinedEvent event) {
        try {
            matching.recomputeForUser(event.eventId(), event.userId());
        } catch (Exception e) {
            log.warn("Incremental recompute failed for event {} user {}: {}",
                event.eventId(), event.userId(), e.toString());
        }
    }

    /** Interest edit → for each event the user is in, recompute pairs (user, *). O(N) per event. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onInterestsChanged(UserInterestsChanged event) {
        List<java.util.UUID> eventIds = participations.findByUserId(event.userId()).stream()
            .map(p -> p.getEventId()).toList();
        for (var eventId : eventIds) {
            try {
                matching.recomputeForUser(eventId, event.userId());
            } catch (Exception e) {
                log.warn("Incremental recompute failed for event {} user {}: {}",
                    eventId, event.userId(), e.toString());
            }
        }
    }
}
