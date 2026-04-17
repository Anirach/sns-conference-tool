package com.sns.event.app;

import com.sns.common.events.MatchRecomputeRequested;
import com.sns.event.api.dto.EventDtos;
import com.sns.event.domain.EventEntity;
import com.sns.event.domain.ParticipationEntity;
import com.sns.event.domain.ParticipationId;
import com.sns.event.repo.EventRepository;
import com.sns.event.repo.ParticipationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

    private static final short[] ALLOWED_RADII = { 20, 50, 100 };
    private static final GeometryFactory GEO = new GeometryFactory(new PrecisionModel(), 4326);

    private final EventRepository events;
    private final ParticipationRepository participations;
    private final QrCodeService qr;
    private final ApplicationEventPublisher publisher;

    public EventService(
        EventRepository events,
        ParticipationRepository participations,
        QrCodeService qr,
        ApplicationEventPublisher publisher
    ) {
        this.events = events;
        this.participations = participations;
        this.qr = qr;
        this.publisher = publisher;
    }

    @Transactional(readOnly = true)
    public List<EventDtos.EventDto> listJoined(UUID userId) {
        return participations.findByUserId(userId).stream()
            .map(p -> events.findById(p.getEventId()).orElse(null))
            .filter(e -> e != null)
            .map(EventService::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public EventDtos.EventDto get(UUID eventId) {
        EventEntity e = events.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toDto(e);
    }

    @Transactional
    public EventDtos.JoinResponse join(UUID userId, String eventCode) {
        EventEntity event = events.findByQrCodeHash(qr.hash(eventCode))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event expired");
        }

        ParticipationId id = new ParticipationId(userId, event.getEventId());
        ParticipationEntity p = participations.findById(id).orElseGet(() -> {
            var np = new ParticipationEntity();
            np.setUserId(userId);
            np.setEventId(event.getEventId());
            np.setSelectedRadius((short) 50);
            return np;
        });
        participations.save(p);
        publisher.publishEvent(new MatchRecomputeRequested(event.getEventId()));
        return new EventDtos.JoinResponse(toDto(event), p.getJoinedAt());
    }

    @Transactional
    public void leave(UUID userId, UUID eventId) {
        participations.deleteByUserIdAndEventId(userId, eventId);
        publisher.publishEvent(new MatchRecomputeRequested(eventId));
    }

    @Transactional
    public void ingestLocation(UUID userId, UUID eventId, EventDtos.LocationRequest req) {
        ParticipationId id = new ParticipationId(userId, eventId);
        ParticipationEntity p = participations.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));

        Point pt = GEO.createPoint(new Coordinate(req.lon(), req.lat()));
        pt.setSRID(4326);
        p.setLastPosition(pt);
        if (req.accuracyMeters() != null) p.setLastPositionAccM(req.accuracyMeters().floatValue());
        p.setLastUpdate(OffsetDateTime.now());
        participations.save(p);
    }

    @Transactional
    public void setRadius(UUID userId, UUID eventId, short radius) {
        if (!isAllowedRadius(radius)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "radius must be 20, 50, or 100");
        }
        ParticipationId id = new ParticipationId(userId, eventId);
        ParticipationEntity p = participations.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        p.setSelectedRadius(radius);
        participations.save(p);
    }

    @Transactional(readOnly = true)
    public short getRadius(UUID userId, UUID eventId) {
        ParticipationId id = new ParticipationId(userId, eventId);
        return participations.findById(id)
            .map(ParticipationEntity::getSelectedRadius)
            .orElse((short) 50);
    }

    private static boolean isAllowedRadius(short r) {
        for (short a : ALLOWED_RADII) if (a == r) return true;
        return false;
    }

    public static EventDtos.EventDto toDto(EventEntity e) {
        return new EventDtos.EventDto(
            e.getEventId(),
            e.getEventName(),
            e.getVenue(),
            e.getExpirationCode().toString(),
            e.getQrCodePlaintext(),
            e.isExpired()
        );
    }
}
