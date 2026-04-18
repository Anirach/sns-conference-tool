package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.identity.app.AuditLogger;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/admin/ops — push outbox queue + key system metrics for the dashboard tile-grid.
 * Reads only; the one write is the manual outbox retry button which simply flips a row to PENDING.
 */
@RestController
@RequestMapping("/api/admin/ops")
public class AdminOpsController {

    private final AdminOpsService service;
    private final AuditLogger audit;

    public AdminOpsController(AdminOpsService service, AuditLogger audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping("/outbox")
    public AdminDtos.Page<AdminDtos.OutboxRow> outbox(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) String status
    ) {
        return service.outbox(page, size, status);
    }

    @PostMapping("/outbox/{outboxId}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retry(@PathVariable UUID outboxId) {
        service.retry(outboxId);
        audit.log("admin.outbox.retry", null, "push_outbox", outboxId.toString());
    }

    @GetMapping("/metrics")
    public AdminDtos.OpsMetrics metrics() {
        return service.metrics();
    }
}
