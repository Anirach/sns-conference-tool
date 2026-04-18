package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.identity.repo.AuditLogRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/admin/audit — read-only paged search over the immutable {@code audit_log} table
 * (V9 trigger blocks UPDATE/DELETE outside the prune job). All filter params optional.
 */
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditLogRepository repo;

    public AdminAuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public AdminDtos.Page<AdminDtos.AuditEntry> search(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) UUID actor,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) OffsetDateTime since,
        @RequestParam(required = false) OffsetDateTime until
    ) {
        // Postgres needs an explicit timestamp on the bind side — substitute wide-but-valid
        // sentinels (well within timestamptz range) when the caller didn't provide a bound.
        OffsetDateTime sinceBound = since == null
            ? OffsetDateTime.parse("1970-01-01T00:00:00Z") : since;
        OffsetDateTime untilBound = until == null
            ? OffsetDateTime.parse("2200-01-01T00:00:00Z") : until;
        var p = repo.search(actor, action, sinceBound, untilBound, PageRequest.of(page, size));
        var items = p.getContent().stream().map(a -> new AdminDtos.AuditEntry(
            UUID.nameUUIDFromBytes(("audit:" + a.getId()).getBytes()),
            a.getActorUserId(), a.getAction(), a.getResourceType(),
            a.getResourceId(),
            a.getPayload() == null ? null : a.getPayload().toString(),
            a.getCreatedAt())).toList();
        return new AdminDtos.Page<>(items, p.getTotalElements(), page, size);
    }
}
