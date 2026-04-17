package com.sns.app.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.chat.app.ChatService;
import com.sns.chat.repo.ChatMessageRepository;
import com.sns.interest.app.InterestService;
import com.sns.matching.repo.SimilarityMatchRepository;
import com.sns.profile.app.ProfileService;
import com.sns.profile.api.dto.UserDto;
import com.sns.sns.app.SnsService;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * GDPR data export. Streams a ZIP with one JSON file per domain: profile, interests, matches,
 * chats, sns-links. Phase 4 aggregator replaces the Phase 1 profile-only export.
 */
@RestController
@RequestMapping("/api/users/me")
public class ExportController {

    private final ProfileService profiles;
    private final InterestService interests;
    private final SimilarityMatchRepository matches;
    private final ChatMessageRepository chatMessages;
    private final ChatService chatService;
    private final SnsService snsService;
    private final ObjectMapper mapper;

    public ExportController(
        ProfileService profiles,
        InterestService interests,
        SimilarityMatchRepository matches,
        ChatMessageRepository chatMessages,
        ChatService chatService,
        SnsService snsService,
        ObjectMapper mapper
    ) {
        this.profiles = profiles;
        this.interests = interests;
        this.matches = matches;
        this.chatMessages = chatMessages;
        this.chatService = chatService;
        this.snsService = snsService;
        this.mapper = mapper;
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(JwtAuthenticationToken auth) {
        UUID userId = UUID.fromString(auth.getToken().getSubject());

        UserDto me = profiles.get(userId);
        var userInterests = interests.listFor(userId);
        var userMatches = matches.findAll().stream()
            .filter(m -> m.getUserIdA().equals(userId) || m.getUserIdB().equals(userId))
            .map(m -> Map.<String, Object>of(
                "matchId", m.getMatchId().toString(),
                "eventId", m.getEventId().toString(),
                "otherUserId", (m.getUserIdA().equals(userId) ? m.getUserIdB() : m.getUserIdA()).toString(),
                "similarity", m.getSimilarity(),
                "commonKeywords", java.util.List.of(m.getCommonKeywords()),
                "createdAt", m.getCreatedAt().toString()
            ))
            .collect(Collectors.toList());
        var userChats = chatService.threadHeads(userId);
        var messages = chatMessages.findAll().stream()
            .filter(m -> m.getFromUserId().equals(userId) || m.getToUserId().equals(userId))
            .map(ChatService::toDto)
            .toList();

        Map<String, Object> snsExport = snsService.buildExport(userId);

        StreamingResponseBody body = (OutputStream out) -> {
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                writeJson(zip, "profile.json", me);
                writeJson(zip, "interests.json", userInterests);
                writeJson(zip, "matches.json", userMatches);
                writeJson(zip, "chat-threads.json", userChats);
                writeJson(zip, "chat-messages.json", messages);
                writeJson(zip, "sns-links.json", snsExport);
                writeManifest(zip, userId);
            }
        };

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sns-export.zip\"")
            .body(body);
    }

    @DeleteMapping
    public Map<String, Object> softDelete(JwtAuthenticationToken auth) {
        profiles.softDelete(UUID.fromString(auth.getToken().getSubject()));
        return Map.of("ok", true, "status", "soft-deleted");
    }

    private void writeJson(ZipOutputStream zip, String name, Object payload) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload));
        zip.closeEntry();
    }

    private void writeManifest(ZipOutputStream zip, UUID userId) throws IOException {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("userId", userId.toString());
        manifest.put("exportedAt", java.time.OffsetDateTime.now().toString());
        manifest.put("schemaVersion", 1);
        writeJson(zip, "manifest.json", manifest);
    }
}
