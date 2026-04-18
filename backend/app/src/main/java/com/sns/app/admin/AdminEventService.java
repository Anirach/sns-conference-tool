package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.chat.repo.ChatMessageRepository;
import com.sns.event.app.QrCodeService;
import com.sns.event.domain.EventEntity;
import com.sns.event.domain.ParticipationEntity;
import com.sns.event.repo.EventRepository;
import com.sns.event.repo.ParticipationRepository;
import com.sns.matching.repo.SimilarityMatchRepository;
import com.sns.profile.domain.ProfileEntity;
import com.sns.profile.repo.ProfileRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-mostly admin operations for events. Writes (create/update/delete) cascade FKs in Postgres
 * (V4 + V5 + V6 migrations), so deleting an event also drops every participation, similarity
 * match, and chat message tied to it — admin UI surfaces a confirmation dialog before invoking.
 */
@Service
public class AdminEventService {

    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    private final EventRepository events;
    private final ParticipationRepository participations;
    private final ProfileRepository profiles;
    private final SimilarityMatchRepository matches;
    private final ChatMessageRepository chats;
    private final QrCodeService qr;

    public AdminEventService(
        EventRepository events,
        ParticipationRepository participations,
        ProfileRepository profiles,
        SimilarityMatchRepository matches,
        ChatMessageRepository chats,
        QrCodeService qr
    ) {
        this.events = events;
        this.participations = participations;
        this.profiles = profiles;
        this.matches = matches;
        this.chats = chats;
        this.qr = qr;
    }

    @Transactional(readOnly = true)
    public AdminDtos.Page<AdminDtos.EventSummary> list(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var p = (q == null || q.isBlank())
            ? events.findAll(pageable)
            : events.findByQuery("%" + q.trim().toLowerCase() + "%", pageable);
        Map<UUID, Long> counts = participantCounts(p.getContent().stream().map(EventEntity::getEventId).toList());
        var items = p.getContent().stream()
            .map(e -> new AdminDtos.EventSummary(
                e.getEventId(), e.getEventName(), e.getVenue(), e.getQrCodePlaintext(),
                e.getExpirationCode(), e.isExpired(), counts.getOrDefault(e.getEventId(), 0L)))
            .toList();
        return new AdminDtos.Page<>(items, p.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public AdminDtos.EventDetail get(UUID eventId) {
        EventEntity e = events.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        long participantCount = participations.findByEventId(eventId).size();
        long matchCount = matches.findByEventId(eventId).size();
        long messageCount = chats.countByEventId(eventId);
        Double lat = e.getCentroid() == null ? null : e.getCentroid().getY();
        Double lon = e.getCentroid() == null ? null : e.getCentroid().getX();
        return new AdminDtos.EventDetail(
            e.getEventId(), e.getEventName(), e.getVenue(), e.getQrCodePlaintext(),
            e.getExpirationCode(), e.isExpired(), lat, lon,
            participantCount, matchCount, messageCount);
    }

    @Transactional
    public AdminDtos.EventDetail create(AdminDtos.EventCreateRequest req) {
        String code = req.qrCodePlaintext().trim().toUpperCase();
        String hash = qr.hash(code);
        if (events.findByQrCodeHash(hash).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "QR code already in use");
        }
        EventEntity e = new EventEntity();
        e.setEventName(req.name());
        e.setVenue(req.venue());
        e.setExpirationCode(req.expirationCode());
        e.setQrCodeHash(hash);
        e.setQrCodePlaintext(code);
        applyCentroid(e, req.centroidLat(), req.centroidLon());
        events.save(e);
        return get(e.getEventId());
    }

    @Transactional
    public AdminDtos.EventDetail update(UUID eventId, AdminDtos.EventUpdateRequest req) {
        EventEntity e = events.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        e.setEventName(req.name());
        e.setVenue(req.venue());
        e.setExpirationCode(req.expirationCode());
        applyCentroid(e, req.centroidLat(), req.centroidLon());
        events.save(e);
        return get(eventId);
    }

    @Transactional
    public void delete(UUID eventId) {
        if (!events.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // FK cascades wipe participations, similarity_matches, chat_messages — all per V4/V5/V6.
        events.deleteById(eventId);
    }

    @Transactional(readOnly = true)
    public AdminDtos.Page<AdminDtos.ParticipantSummary> participants(UUID eventId, int page, int size) {
        if (!events.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // No native "page" repo method on participations (PK is composite), so we sort + slice in memory.
        // Participant counts per event are bounded (hundreds at worst) so paging client-side is fine.
        List<ParticipationEntity> all = participations.findByEventId(eventId);
        all.sort((a, b) -> b.getJoinedAt().compareTo(a.getJoinedAt()));
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<ParticipationEntity> slice = all.subList(from, to);
        Map<UUID, ProfileEntity> profileById = new HashMap<>();
        for (var p : slice) {
            profiles.findById(p.getUserId()).ifPresent(pf -> profileById.put(p.getUserId(), pf));
        }
        var items = slice.stream()
            .map(p -> {
                ProfileEntity pf = profileById.get(p.getUserId());
                Double lat = p.getLastPosition() == null ? null : p.getLastPosition().getY();
                Double lon = p.getLastPosition() == null ? null : p.getLastPosition().getX();
                return new AdminDtos.ParticipantSummary(
                    p.getUserId(),
                    pf == null ? null : pf.getFirstName(),
                    pf == null ? null : pf.getLastName(),
                    pf == null ? null : pf.getInstitution(),
                    lat, lon, p.getLastUpdate(), p.getSelectedRadius());
            })
            .toList();
        return new AdminDtos.Page<>(items, all.size(), page, size);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.HeatmapPoint> heatmap(UUID eventId) {
        if (!events.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return participations.findByEventId(eventId).stream()
            .filter(p -> p.getLastPosition() != null)
            .map(p -> new AdminDtos.HeatmapPoint(
                p.getLastPosition().getY(), p.getLastPosition().getX(), p.getLastUpdate()))
            .toList();
    }

    private static void applyCentroid(EventEntity e, Double lat, Double lon) {
        if (lat == null || lon == null) {
            e.setCentroid(null);
            return;
        }
        Point pt = GEO.createPoint(new Coordinate(lon, lat));
        pt.setSRID(4326);
        e.setCentroid(pt);
    }

    private Map<UUID, Long> participantCounts(List<UUID> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        Map<UUID, Long> counts = new HashMap<>();
        for (UUID id : eventIds) {
            counts.put(id, (long) participations.findByEventId(id).size());
        }
        return counts;
    }
}
