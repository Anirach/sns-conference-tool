package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.identity.app.AuditLogger;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/admin/events — gated by {@code .hasAnyRole("ADMIN","SUPER_ADMIN")} in
 * {@code SecurityConfig}. Every write emits an audit row before commit.
 */
@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final AdminEventService service;
    private final AuditLogger audit;

    public AdminEventController(AdminEventService service, AuditLogger audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    public AdminDtos.Page<AdminDtos.EventSummary> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q
    ) {
        return service.list(page, size, q);
    }

    @GetMapping("/{eventId}")
    public AdminDtos.EventDetail get(@PathVariable UUID eventId) {
        return service.get(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDtos.EventDetail create(@Valid @RequestBody AdminDtos.EventCreateRequest req) {
        var created = service.create(req);
        audit.log("admin.event.created", null, "event", created.eventId().toString());
        return created;
    }

    @PutMapping("/{eventId}")
    public AdminDtos.EventDetail update(@PathVariable UUID eventId, @Valid @RequestBody AdminDtos.EventUpdateRequest req) {
        var updated = service.update(eventId, req);
        audit.log("admin.event.updated", null, "event", eventId.toString());
        return updated;
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId) {
        audit.log("admin.event.deleted", null, "event", eventId.toString());
        service.delete(eventId);
    }

    @GetMapping("/{eventId}/participants")
    public AdminDtos.Page<AdminDtos.ParticipantSummary> participants(
        @PathVariable UUID eventId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return service.participants(eventId, page, size);
    }

    @GetMapping("/{eventId}/heatmap")
    public List<AdminDtos.HeatmapPoint> heatmap(@PathVariable UUID eventId) {
        return service.heatmap(eventId);
    }
}
