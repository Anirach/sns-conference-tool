package com.sns.identity.app;

import com.sns.identity.domain.AuditLogEntity;
import com.sns.identity.repo.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Single entry point for writing audit rows. All writes happen in a {@code REQUIRES_NEW}
 * transaction so the audit trail is never rolled back alongside the operation it records.
 * <p>
 * The caller's IP is SHA-256-hashed with a per-deployment salt before storage so the GDPR export
 * surface stays free of raw IP addresses. Emails and other obvious PII must never appear in the
 * payload map — callers are expected to pass {@code userId} references instead.
 *
 * <p>Canonical action names (matching the spec's §11.7 event catalog):
 * <ul>
 *   <li>{@code auth.register}, {@code auth.verify}, {@code auth.complete}, {@code auth.login},
 *       {@code auth.refresh}, {@code auth.logout}</li>
 *   <li>{@code profile.update}, {@code profile.soft_delete}, {@code profile.hard_delete}</li>
 *   <li>{@code sns.link}, {@code sns.callback}, {@code sns.unlink}, {@code sns.enrich}</li>
 *   <li>{@code export.download}</li>
 * </ul>
 */
@Component
public class AuditLogger {

    private final AuditLogRepository repo;
    private final byte[] ipSalt;

    public AuditLogger(
        AuditLogRepository repo,
        @Value("${sns.audit.ip-salt:dev-audit-ip-salt-change-me}") String ipSalt
    ) {
        this.repo = repo;
        this.ipSalt = ipSalt.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, UUID actorUserId, String resourceType, String resourceId, Map<String, Object> payload) {
        AuditLogEntity row = new AuditLogEntity();
        row.setAction(action);
        row.setActorUserId(actorUserId);
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setIpHash(currentRequestIpHash());
        row.setPayload(payload == null ? Map.of() : scrub(payload));
        repo.save(row);
    }

    public void log(String action, UUID actorUserId) {
        log(action, actorUserId, null, null, null);
    }

    public void log(String action, UUID actorUserId, String resourceType, String resourceId) {
        log(action, actorUserId, resourceType, resourceId, null);
    }

    private static Map<String, Object> scrub(Map<String, Object> in) {
        Map<String, Object> out = new HashMap<>(in.size());
        for (var e : in.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String s) {
                out.put(e.getKey(), PiiScrubber.mask(s));
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }

    private String currentRequestIpHash() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String ip = firstNonBlank(req.getHeader("X-Forwarded-For"), req.getRemoteAddr());
        if (ip == null) return null;
        if (ip.contains(",")) ip = ip.substring(0, ip.indexOf(',')).trim();
        return hashIp(ip);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private String hashIp(String ip) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(ipSalt);
            return HexFormat.of().formatHex(md.digest(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) return sra.getRequest();
        return null;
    }

}
