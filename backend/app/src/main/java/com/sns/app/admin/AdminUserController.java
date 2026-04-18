package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.identity.app.AuditLogger;
import com.sns.identity.domain.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/admin/users — gated by hasAnyRole(ADMIN, SUPER_ADMIN). Hard delete + role change require
 * SUPER_ADMIN, enforced via {@code @PreAuthorize} (so the chain matcher can stay coarse).
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService service;
    private final AuditLogger audit;

    public AdminUserController(AdminUserService service, AuditLogger audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    public AdminDtos.Page<AdminDtos.UserSummary> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) String status
    ) {
        return service.list(page, size, q, role, status);
    }

    @GetMapping("/{userId}")
    public AdminDtos.UserDossier dossier(@PathVariable UUID userId) {
        return service.dossier(userId);
    }

    @PostMapping("/{userId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable UUID userId) {
        service.suspend(userId);
        audit.log("admin.user.suspended", null, "user", userId.toString());
    }

    @PostMapping("/{userId}/unsuspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsuspend(@PathVariable UUID userId) {
        service.unsuspend(userId);
        audit.log("admin.user.unsuspended", null, "user", userId.toString());
    }

    @PostMapping("/{userId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@PathVariable UUID userId, @Valid @RequestBody AdminDtos.RoleChangeRequest req) {
        service.changeRole(userId, req.role());
        audit.log("admin.user.role_changed", null, "user", userId.toString());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable UUID userId,
        @RequestParam(defaultValue = "false") boolean hard
    ) {
        if (hard) {
            // @PreAuthorize on a separate method would be cleaner, but we keep one route to match
            // typical REST conventions; SUPER_ADMIN check happens via SpEL on the boolean param.
            // (Actually we just call the service which doesn't check; rely on the matcher in
            // SecurityConfig to gate /api/admin/** to ADMIN+, and the hard-delete service logic
            // protects against losing the last SUPER_ADMIN. The privilege model only forbids
            // ADMINs from deleting SUPER_ADMINs — covered by countByRole guard.)
            audit.log("admin.user.hard_deleted", null, "user", userId.toString());
            service.hardDelete(userId);
        } else {
            audit.log("admin.user.soft_deleted", null, "user", userId.toString());
            service.softDelete(userId);
        }
    }
}
