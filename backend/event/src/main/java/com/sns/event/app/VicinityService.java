package com.sns.event.app;

import com.sns.event.api.dto.EventDtos.MatchDto;
import java.sql.Array;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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

    private static final String QUERY = """
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
        JOIN participations me
          ON me.event_id = peer.event_id
         AND me.user_id  = ?
        LEFT JOIN profiles prof ON prof.user_id = peer.user_id
        LEFT JOIN similarity_matches m
          ON m.event_id = peer.event_id
         AND m.user_id_a = LEAST(peer.user_id, me.user_id)
         AND m.user_id_b = GREATEST(peer.user_id, me.user_id)
        WHERE peer.event_id = ?
          AND peer.user_id <> me.user_id
          AND peer.last_update > now() - INTERVAL '5 minutes'
          AND me.last_position IS NOT NULL
          AND peer.last_position IS NOT NULL
          AND ST_DWithin(peer.last_position, me.last_position, ?)
        ORDER BY similarity DESC NULLS LAST, distance_meters ASC
        LIMIT 200
        """;

    private final JdbcTemplate jdbc;

    public VicinityService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
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
        }, userId, eventId, (double) radiusMeters);
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
