package com.sns.event.app;

import com.sns.common.events.LocationUpdated;
import com.sns.common.events.MatchRecomputeRequested;
import com.sns.event.api.dto.EventDtos.MatchDto;
import java.sql.Array;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads nearby-peers-with-matches for the current user. This is the hot path on the vicinity screen.
 * <p>
 * The query joins {@code participations} on self+peer, filters peers whose last GPS update is within
 * 5 minutes, applies {@code ST_DWithin} against the selected radius, then LEFT JOINs
 * {@code similarity_matches} (canonical ordering on the pair) so the response carries the pre-computed
 * similarity + common_keywords without triggering a recompute on the request path.
 */
@Service
public class VicinityService {

    /**
     * The {@code similarity_matches} table enforces {@code user_id_a < user_id_b} via a CHECK
     * constraint, so the canonical pair lookup avoids {@code LEAST/GREATEST} (which defeat the
     * unique B-tree index {@code idx_matches_pair}) by branching on the lexicographic order of
     * peer vs. me. The OR expands to two structurally identical lookups, each one of which
     * the planner can satisfy with a direct index probe.
     * <p>
     * The CTE also avoids re-reading {@code me}'s row from {@code participations} once per
     * candidate.
     */
    private static final String QUERY = """
        WITH me AS (
          SELECT user_id, last_position
          FROM participations
          WHERE event_id = ?::uuid AND user_id = ?::uuid
        )
        SELECT
          m.match_id,
          peer.user_id  AS other_user_id,
          COALESCE(m.similarity, 0)  AS similarity,
          COALESCE(m.common_keywords, '{}'::text[]) AS common_keywords,
          COALESCE(m.mutual, false)  AS mutual,
          ST_Distance(peer.last_position, me.last_position) AS distance_meters,
          prof.first_name,
          prof.last_name,
          prof.academic_title,
          prof.institution,
          prof.profile_picture_url
        FROM participations peer
        CROSS JOIN me
        LEFT JOIN profiles prof ON prof.user_id = peer.user_id
        LEFT JOIN similarity_matches m
          ON m.event_id = ?::uuid
         AND ((peer.user_id < me.user_id
               AND m.user_id_a = peer.user_id
               AND m.user_id_b = me.user_id)
           OR (peer.user_id > me.user_id
               AND m.user_id_a = me.user_id
               AND m.user_id_b = peer.user_id))
        WHERE peer.event_id = ?::uuid
          AND peer.user_id <> me.user_id
          AND peer.last_update > now() - INTERVAL '5 minutes'
          AND me.last_position IS NOT NULL
          AND peer.last_position IS NOT NULL
          AND ST_DWithin(peer.last_position, me.last_position, ?)
        ORDER BY similarity DESC NULLS LAST, distance_meters ASC
        LIMIT 200
        """;

    private final JdbcTemplate jdbc;
    private final CacheManager cacheManager;

    public VicinityService(JdbcTemplate jdbc, CacheManager cacheManager) {
        this.jdbc = jdbc;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "vicinity", key = "#eventId + ':' + #userId + ':' + #radiusMeters")
    public List<MatchDto> matchesInRadius(UUID eventId, UUID userId, short radiusMeters) {
        return jdbc.query(QUERY, (rs, i) -> {
            UUID matchId = (UUID) rs.getObject("match_id");
            UUID otherId = (UUID) rs.getObject("other_user_id");
            Array kwArr = rs.getArray("common_keywords");
            List<String> commonKeywords = kwArr == null
                ? Collections.emptyList()
                : List.of((String[]) kwArr.getArray());
            String name = formatName(rs.getString("first_name"), rs.getString("last_name"));
            return new MatchDto(
                matchId,
                eventId,
                otherId,
                name,
                rs.getString("academic_title"),
                rs.getString("institution"),
                rs.getString("profile_picture_url"),
                commonKeywords,
                rs.getFloat("similarity"),
                rs.getBoolean("mutual"),
                rs.getDouble("distance_meters")
            );
        }, eventId, userId, eventId, eventId, (double) radiusMeters);
    }

    /**
     * Blanket evict on any event-level change. The cache key includes userId + radius so it
     * holds many entries per event; clearing the whole cache occasionally is cheaper than
     * tracking per-user keys and safer (never returns stale similarity for 10 s after a match
     * upsert or fresh location fix).
     */
    @EventListener
    public void onMatchRecompute(MatchRecomputeRequested e) {
        var cache = cacheManager.getCache("vicinity");
        if (cache != null) cache.clear();
    }

    @EventListener
    public void onLocationUpdated(LocationUpdated e) {
        var cache = cacheManager.getCache("vicinity");
        if (cache != null) cache.clear();
    }

    private static String formatName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) sb.append(first);
        if (last != null && !last.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(last);
        }
        return sb.length() == 0 ? "Anonymous" : sb.toString();
    }
}
