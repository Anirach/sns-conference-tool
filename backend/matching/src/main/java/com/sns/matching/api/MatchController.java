package com.sns.matching.api;

import com.sns.event.api.dto.EventDtos;
import com.sns.matching.domain.SimilarityMatchEntity;
import com.sns.matching.repo.SimilarityMatchRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final SimilarityMatchRepository matches;
    private final JdbcTemplate jdbc;

    public MatchController(SimilarityMatchRepository matches, JdbcTemplate jdbc) {
        this.matches = matches;
        this.jdbc = jdbc;
    }

    @GetMapping("/{matchId}")
    public EventDtos.MatchDto get(JwtAuthenticationToken auth, @PathVariable UUID matchId) {
        SimilarityMatchEntity m = matches.findById(matchId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UUID me = UUID.fromString(auth.getToken().getSubject());
        if (!m.getUserIdA().equals(me) && !m.getUserIdB().equals(me)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        UUID other = m.getUserIdA().equals(me) ? m.getUserIdB() : m.getUserIdA();

        return jdbc.query("""
            SELECT prof.first_name, prof.last_name, prof.academic_title, prof.institution, prof.profile_picture_url
            FROM profiles prof WHERE prof.user_id = ?
            """, rs -> {
                String first = null, last = null, title = null, inst = null, pic = null;
                if (rs.next()) {
                    first = rs.getString("first_name");
                    last = rs.getString("last_name");
                    title = rs.getString("academic_title");
                    inst = rs.getString("institution");
                    pic = rs.getString("profile_picture_url");
                }
                String name = formatName(first, last);
                return new EventDtos.MatchDto(
                    m.getMatchId(),
                    m.getEventId(),
                    other,
                    name,
                    title,
                    inst,
                    pic,
                    List.of(m.getCommonKeywords()),
                    m.getSimilarity(),
                    m.isMutual(),
                    0.0
                );
            }, other);
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
