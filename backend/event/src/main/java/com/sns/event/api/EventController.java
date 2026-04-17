package com.sns.event.api;

import com.sns.event.api.dto.EventDtos;
import com.sns.event.app.EventService;
import com.sns.event.app.VicinityService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService events;
    private final VicinityService vicinity;

    public EventController(EventService events, VicinityService vicinity) {
        this.events = events;
        this.vicinity = vicinity;
    }

    @GetMapping("/joined")
    public Object joined(JwtAuthenticationToken auth) {
        return events.listJoined(userId(auth));
    }

    @GetMapping("/{eventId}")
    public EventDtos.EventDto get(@PathVariable UUID eventId) {
        return events.get(eventId);
    }

    @PostMapping("/join")
    public EventDtos.JoinResponse join(JwtAuthenticationToken auth, @Valid @RequestBody EventDtos.JoinRequest req) {
        return events.join(userId(auth), req.eventCode());
    }

    @PostMapping("/{eventId}/leave")
    public Map<String, Object> leave(JwtAuthenticationToken auth, @PathVariable UUID eventId) {
        events.leave(userId(auth), eventId);
        return Map.of("ok", true);
    }

    @GetMapping("/{eventId}/vicinity")
    public EventDtos.VicinityResponse vicinity(
        JwtAuthenticationToken auth,
        @PathVariable UUID eventId,
        @RequestParam(name = "radius", required = false) Short radius
    ) {
        UUID uid = userId(auth);
        short effectiveRadius = radius != null ? radius : events.getRadius(uid, eventId);
        var matches = vicinity.matchesInRadius(eventId, uid, effectiveRadius);
        return new EventDtos.VicinityResponse(effectiveRadius, matches);
    }

    @PostMapping("/{eventId}/location")
    public ResponseEntity<Void> location(
        JwtAuthenticationToken auth,
        @PathVariable UUID eventId,
        @RequestBody EventDtos.LocationRequest req
    ) {
        events.ingestLocation(userId(auth), eventId, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{eventId}/radius")
    public Map<String, Object> radius(
        JwtAuthenticationToken auth,
        @PathVariable UUID eventId,
        @RequestBody EventDtos.RadiusRequest req
    ) {
        events.setRadius(userId(auth), eventId, req.radius());
        return Map.of("ok", true);
    }

    private static UUID userId(JwtAuthenticationToken auth) {
        return UUID.fromString(auth.getToken().getSubject());
    }
}
