package com.sns.app.admin;

import com.sns.app.admin.dto.AdminDtos;
import com.sns.chat.repo.ChatMessageRepository;
import com.sns.event.domain.ParticipationEntity;
import com.sns.event.repo.EventRepository;
import com.sns.event.repo.ParticipationRepository;
import com.sns.identity.domain.Role;
import com.sns.identity.domain.UserEntity;
import com.sns.identity.repo.AuditLogRepository;
import com.sns.identity.repo.UserRepository;
import com.sns.interest.domain.InterestEntity;
import com.sns.interest.repo.InterestRepository;
import com.sns.matching.repo.SimilarityMatchRepository;
import com.sns.notification.repo.DeviceTokenRepository;
import com.sns.profile.domain.ProfileEntity;
import com.sns.profile.repo.ProfileRepository;
import com.sns.sns.repo.SnsLinkRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final InterestRepository interests;
    private final ParticipationRepository participations;
    private final EventRepository events;
    private final SimilarityMatchRepository matches;
    private final ChatMessageRepository chats;
    private final DeviceTokenRepository devices;
    private final SnsLinkRepository snsLinks;
    private final AuditLogRepository audit;

    public AdminUserService(
        UserRepository users, ProfileRepository profiles, InterestRepository interests,
        ParticipationRepository participations, EventRepository events,
        SimilarityMatchRepository matches, ChatMessageRepository chats,
        DeviceTokenRepository devices, SnsLinkRepository snsLinks,
        AuditLogRepository audit
    ) {
        this.users = users; this.profiles = profiles; this.interests = interests;
        this.participations = participations; this.events = events;
        this.matches = matches; this.chats = chats; this.devices = devices;
        this.snsLinks = snsLinks; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AdminDtos.Page<AdminDtos.UserSummary> list(int page, int size, String q, Role role, String status) {
        Pageable pageable = PageRequest.of(page, size);
        String searchPattern = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        var p = users.searchAdmin(searchPattern, role, normaliseStatus(status), pageable);
        var items = p.getContent().stream().map(u -> {
            ProfileEntity pf = profiles.findById(u.getUserId()).orElse(null);
            return new AdminDtos.UserSummary(
                u.getUserId(), u.getEmail(),
                pf == null ? null : pf.getFirstName(),
                pf == null ? null : pf.getLastName(),
                pf == null ? null : pf.getInstitution(),
                u.getRole(),
                u.getSuspendedAt() != null,
                u.getDeletedAt() != null,
                u.getCreatedAt());
        }).toList();
        return new AdminDtos.Page<>(items, p.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public AdminDtos.UserDossier dossier(UUID userId) {
        UserEntity u = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ProfileEntity pf = profiles.findById(userId).orElse(null);
        List<InterestEntity> ints = interests.findByUserIdOrderByCreatedAtDesc(userId);
        List<ParticipationEntity> parts = participations.findByUserId(userId);
        long matchCount = matches.findByUserIdAOrUserIdB(userId, userId).size();
        long messageCount = chats.countByFromUserIdOrToUserId(userId, userId);
        long deviceCount = devices.findByUserId(userId).size();
        long snsCount = snsLinks.findByUserId(userId).size();
        var recentAudit = audit.findByActorUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
            .getContent().stream()
            .map(a -> new AdminDtos.AuditEntry(
                java.util.UUID.nameUUIDFromBytes(("audit:" + a.getId()).getBytes()),
                a.getActorUserId(), a.getAction(), a.getResourceType(),
                a.getResourceId(),
                a.getPayload() == null ? null : a.getPayload().toString(),
                a.getCreatedAt()))
            .toList();

        var interestSummaries = ints.stream().map(i -> new AdminDtos.InterestSummary(
            i.getInterestId(), i.getType().name(), i.getContent(),
            java.util.Arrays.asList(i.getExtractedKeywords()), i.getCreatedAt())).toList();

        var joined = parts.stream().map(p -> {
            String name = events.findById(p.getEventId()).map(e -> e.getEventName()).orElse("(unknown)");
            return new AdminDtos.JoinedEvent(p.getEventId(), name, p.getJoinedAt(), p.getSelectedRadius());
        }).toList();

        return new AdminDtos.UserDossier(
            u.getUserId(), u.getEmail(), u.getRole(),
            u.getSuspendedAt() != null, u.getDeletedAt() != null,
            u.getCreatedAt(), u.getSuspendedAt(), u.getDeletedAt(),
            pf == null ? null : pf.getFirstName(),
            pf == null ? null : pf.getLastName(),
            pf == null ? null : pf.getAcademicTitle(),
            pf == null ? null : pf.getInstitution(),
            pf == null ? null : pf.getProfilePictureUrl(),
            interestSummaries, joined,
            matchCount, messageCount, deviceCount, snsCount,
            recentAudit);
    }

    @Transactional
    public void suspend(UUID userId) {
        UserEntity u = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getSuspendedAt() == null) {
            u.setSuspendedAt(OffsetDateTime.now());
            users.save(u);
        }
    }

    @Transactional
    public void unsuspend(UUID userId) {
        UserEntity u = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getSuspendedAt() != null) {
            u.setSuspendedAt(null);
            users.save(u);
        }
    }

    /**
     * Role change. Caller must be SUPER_ADMIN — enforced in the controller via @PreAuthorize.
     * Refuses to demote the last SUPER_ADMIN so the system can never be locked out of /admin.
     */
    @Transactional
    public void changeRole(UUID userId, Role newRole) {
        UserEntity u = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getRole() == Role.SUPER_ADMIN && newRole != Role.SUPER_ADMIN) {
            long remaining = users.countByRole(Role.SUPER_ADMIN);
            if (remaining <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot demote the last SUPER_ADMIN");
            }
        }
        u.setRole(newRole);
        users.save(u);
    }

    @Transactional
    public void softDelete(UUID userId) {
        UserEntity u = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getDeletedAt() == null) {
            u.setDeletedAt(OffsetDateTime.now());
            users.save(u);
        }
    }

    @Transactional
    public void hardDelete(UUID userId) {
        UserEntity u = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getRole() == Role.SUPER_ADMIN) {
            long remaining = users.countByRole(Role.SUPER_ADMIN);
            if (remaining <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot hard-delete the last SUPER_ADMIN");
            }
        }
        // FK cascades wipe profile, interests, participations, matches, chat, devices, refresh tokens, sns_links.
        users.deleteById(userId);
    }

    private static String normaliseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String s = status.trim().toLowerCase();
        return switch (s) {
            case "active", "suspended", "deleted" -> s;
            default -> null;
        };
    }
}
