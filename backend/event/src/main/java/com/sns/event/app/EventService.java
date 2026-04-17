package com.sns.event.app;

import com.sns.common.events.MatchRecomputeRequested;
import com.sns.event.api.dto.EventDtos;
import com.sns.event.domain.EventEntity;
import com.sns.event.domain.ParticipationEntity;
import com.sns.event.domain.ParticipationId;
import com.sns.event.repo.EventRepository;
import com.sns.event.repo.ParticipationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
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
    private final Counter throttledCounter;
    private final long throttleSeconds;
    private final double throttleMinMoveMeters;

    public EventService(
        EventRepository events,
        ParticipationRepository participations,
        QrCodeService qr,
        ApplicationEventPublisher publisher,
        MeterRegistry meterRegistry,
        @Value("${sns.location.throttle-seconds:30}") long throttleSeconds,
        @Value("${sns.location.throttle-min-move-meters:10}") double throttleMinMoveMeters
    ) {
        this.events = events;
        this.participations = participations;
        this.qr = qr;
        this.publisher = publisher;
        this.throttledCounter = Counter.builder("sns_location_throttled")
            .description("Location ingest requests rejected by the server-side throttle")
            .register(meterRegistry);
        this.throttleSeconds = throttleSeconds;
        this.throttleMinMoveMeters = throttleMinMoveMeters;
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
        // Signed QR tokens carry a '.' between payload and signature. If present, verify first and
        // fall back to the legacy hash lookup using the extracted plaintext.
        String code = eventCode;
        if (code != null && code.contains(".")) {
            String verified = qr.tryVerify(code);
            if (verified != null) code = verified;
        }
        EventEntity event = events.findByQrCodeHash(qr.hash(code))
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
    public boolean ingestLocation(UUID userId, UUID eventId, EventDtos.LocationRequest req) {
        ParticipationId id = new ParticipationId(userId, eventId);
        ParticipationEntity p = participations.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));

        Point pt = GEO.createPoint(new Coordinate(req.lon(), req.lat()));
        pt.setSRID(4326);
        OffsetDateTime now = OffsetDateTime.now();

        // Server-side throttle — drop silently if the previous fix was recent AND the user
        // hasn't moved meaningfully. Protects the DB and PostGIS index from chatty clients.
        if (shouldThrottle(p, pt, now)) {
            throttledCounter.increment();
            return false;
        }

        p.setLastPosition(pt);
        if (req.accuracyMeters() != null) p.setLastPositionAccM(req.accuracyMeters().floatValue());
        p.setLastUpdate(now);
        participations.save(p);
        publisher.publishEvent(new com.sns.common.events.LocationUpdated(eventId, userId));
        return true;
    }

    private boolean shouldThrottle(ParticipationEntity p, Point incoming, OffsetDateTime now) {
        if (p.getLastUpdate() == null || p.getLastPosition() == null) return false;
        long secondsSince = Duration.between(p.getLastUpdate(), now).toSeconds();
        if (secondsSince >= throttleSeconds) return false;
        double movedMeters = haversineMeters(
            p.getLastPosition().getY(), p.getLastPosition().getX(),
            incoming.getY(), incoming.getX());
        return movedMeters < throttleMinMoveMeters;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
              * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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
