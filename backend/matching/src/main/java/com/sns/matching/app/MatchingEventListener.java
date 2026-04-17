package com.sns.matching.app;

import com.sns.common.events.MatchRecomputeRequested;
import com.sns.common.events.UserInterestsChanged;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onParticipationChanged(MatchRecomputeRequested event) {
        try {
            matching.recompute(event.eventId());
        } catch (Exception e) {
            log.warn("Matching recompute failed for event {}: {}", event.eventId(), e.toString());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onInterestsChanged(UserInterestsChanged event) {
        List<java.util.UUID> eventIds = participations.findByUserId(event.userId()).stream()
            .map(p -> p.getEventId()).toList();
        for (var eventId : eventIds) {
            try {
                matching.recompute(eventId);
            } catch (Exception e) {
                log.warn("Matching recompute failed for event {} (user {}): {}",
                    eventId, event.userId(), e.toString());
            }
        }
    }
}
