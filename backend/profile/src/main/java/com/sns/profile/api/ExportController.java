package com.sns.profile.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.profile.api.dto.UserDto;
import com.sns.profile.app.ProfileService;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * GDPR data export (Phase 1 version). Streams a ZIP containing the user's profile.
 * Phase 4 will extend this to include interests, matches, chats, and SNS links.
 */
@RestController
@RequestMapping("/api/users/me")
public class ExportController {

    private final ProfileService profiles;
    private final ObjectMapper mapper;

    public ExportController(ProfileService profiles, ObjectMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(JwtAuthenticationToken auth) {
        UUID userId = UUID.fromString(auth.getToken().getSubject());
        UserDto me = profiles.get(userId);

        StreamingResponseBody body = (OutputStream out) -> {
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                writeJson(zip, "profile.json", me);
            }
        };

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sns-export.zip\"")
            .body(body);
    }

    private void writeJson(ZipOutputStream zip, String name, Object payload) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(mapper.writeValueAsBytes(payload));
        zip.closeEntry();
    }
}
