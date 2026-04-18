package com.sns.matching.app;

import com.sns.common.events.MatchFound;
import com.sns.event.domain.ParticipationEntity;
import com.sns.event.repo.EventRepository;
import com.sns.event.repo.ParticipationRepository;
import com.sns.interest.domain.InterestEntity;
import com.sns.interest.repo.InterestRepository;
import com.sns.matching.domain.SimilarityMatchEntity;
import com.sns.matching.repo.SimilarityMatchRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recomputes similarity matches for an event. Each call:
 *   1. Gathers all current participants.
 *   2. Builds one unified keyword vector per user (sum of their interest vectors, then L2-normalised).
 *   3. For every canonical pair (user_a < user_b) computes cosine similarity.
 *   4. Upserts rows with similarity ≥ threshold into {@code similarity_matches}; sets
 *      {@code mutual=true} when both users are still present.
 * <p>
 * Invocation strategy:
 *   - {@link #recompute(UUID)} is called synchronously after a participation or interest change.
 *   - A scheduled sweep runs every few minutes as a safety net.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private static final double SIMILARITY_THRESHOLD = 0.05;
    private static final int COMMON_KEYWORDS_LIMIT = 8;

    private final EventRepository events;
    private final ParticipationRepository participations;
    private final InterestRepository interests;
    private final SimilarityMatchRepository matches;
    private final SimilarityEngine engine;
    private final ApplicationEventPublisher publisher;
    private final long sweepIntervalMs;

    public MatchingService(
        EventRepository events,
        ParticipationRepository participations,
        InterestRepository interests,
        SimilarityMatchRepository matches,
        SimilarityEngine engine,
        ApplicationEventPublisher publisher,
        @Value("${sns.matching.sweep-interval-ms:180000}") long sweepIntervalMs
    ) {
        this.events = events;
        this.participations = participations;
        this.interests = interests;
        this.matches = matches;
        this.engine = engine;
        this.publisher = publisher;
        this.sweepIntervalMs = sweepIntervalMs;
    }

    @Scheduled(fixedDelayString = "${sns.matching.sweep-interval-ms:180000}")
    public void scheduledSweep() {
        List<UUID> active = events.findAll().stream()
            .filter(e -> !e.isExpired())
            .map(e -> e.getEventId())
            .toList();
        for (UUID eventId : active) {
            try {
                recompute(eventId);
            } catch (Exception e) {
                log.warn("recompute failed for event {}: {}", eventId, e.toString());
            }
        }
    }

    /**
     * Full O(N²) recompute. Used only by the scheduled sweep — runtime triggers should prefer
     * {@link #recomputeForUser(UUID, UUID)}.
     */
    @Transactional
    public int recompute(UUID eventId) {
        List<ParticipationEntity> ps = participations.findByEventId(eventId);
        if (ps.size() < 2) return 0;

        Map<UUID, Map<String, Double>> vectorByUser = new HashMap<>(ps.size());
        for (ParticipationEntity p : ps) {
            vectorByUser.put(p.getUserId(), userVector(p.getUserId()));
        }

        int upserts = 0;
        List<UUID> userIds = new ArrayList<>(vectorByUser.keySet());
        userIds.sort(UUID::compareTo);
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                if (upsertPair(eventId, userIds.get(i), userIds.get(j),
                        vectorByUser.get(userIds.get(i)), vectorByUser.get(userIds.get(j)))) {
                    upserts++;
                }
            }
        }
        log.debug("recompute({}) → {} upserts over {} participants", eventId, upserts, userIds.size());
        return upserts;
    }

    /**
     * Incremental recompute: only pairs involving {@code userId}. O(N) where N is the
     * participant count for the event. Driven by {@code UserInterestsChanged} and
     * {@code UserJoinedEvent} so an interest edit or a single join doesn't trigger a full sweep.
     */
    @Transactional
    public int recomputeForUser(UUID eventId, UUID userId) {
        List<ParticipationEntity> peers = participations.findByEventId(eventId);
        if (peers.size() < 2) return 0;

        Map<String, Double> myVector = userVector(userId);
        if (myVector.isEmpty()) {
            log.debug("recomputeForUser({}, {}) → empty vector, skipping", eventId, userId);
            return 0;
        }

        int upserts = 0;
        for (ParticipationEntity p : peers) {
            if (p.getUserId().equals(userId)) continue;
            if (upsertPair(eventId, userId, p.getUserId(), myVector, userVector(p.getUserId()))) {
                upserts++;
            }
        }
        log.debug("recomputeForUser({}, {}) → {} upserts over {} peers", eventId, userId, upserts, peers.size() - 1);
        return upserts;
    }

    private boolean upsertPair(
        UUID eventId,
        UUID first,
        UUID second,
        Map<String, Double> vFirst,
        Map<String, Double> vSecond
    ) {
        boolean firstIsA = first.compareTo(second) < 0;
        UUID a = firstIsA ? first : second;
        UUID b = firstIsA ? second : first;
        Map<String, Double> va = firstIsA ? vFirst : vSecond;
        Map<String, Double> vb = firstIsA ? vSecond : vFirst;

        double sim = engine.cosine(va, vb);
        if (sim < SIMILARITY_THRESHOLD) return false;

        List<String> common = engine.commonKeywords(va, vb, COMMON_KEYWORDS_LIMIT);
        SimilarityMatchEntity m = matches.findByEventIdAndUserIdAAndUserIdB(eventId, a, b)
            .orElseGet(SimilarityMatchEntity::new);
        boolean isNew = (m.getMatchId() == null);
        m.setEventId(eventId);
        m.setUserIdA(a);
        m.setUserIdB(b);
        m.setSimilarity((float) sim);
        m.setCommonKeywords(common.toArray(new String[0]));
        m.setMutual(true);
        var saved = matches.save(m);
        if (isNew) {
            publisher.publishEvent(new MatchFound(saved.getMatchId(), eventId, a, b, (float) sim, common));
        }
        return true;
    }

    private Map<String, Double> userVector(UUID userId) {
        List<InterestEntity> all = interests.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Double> sum = new HashMap<>();
        for (InterestEntity i : all) {
            for (var e : i.getKeywordVector().entrySet()) {
                sum.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        return normalise(sum);
    }

    private static Map<String, Double> normalise(Map<String, Double> v) {
        if (v.isEmpty()) return v;
        double s = 0.0;
        for (double x : v.values()) s += x * x;
        double norm = Math.sqrt(s);
        if (norm == 0.0) return Map.of();
        Map<String, Double> out = new HashMap<>(v.size());
        for (var e : v.entrySet()) out.put(e.getKey(), e.getValue() / norm);
        return out;
    }
}
