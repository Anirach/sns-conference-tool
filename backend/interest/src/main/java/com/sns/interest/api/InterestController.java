package com.sns.interest.api;

import com.sns.interest.api.dto.InterestDtos;
import com.sns.interest.app.InterestService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/interests")
public class InterestController {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final java.util.Set<String> ALLOWED_MIME = java.util.Set.of(
        "application/pdf", "text/plain", "text/markdown"
    );
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private final InterestService service;

    public InterestController(InterestService service) {
        this.service = service;
    }

    @GetMapping
    public Object list(JwtAuthenticationToken auth) {
        return service.listFor(userId(auth));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InterestDtos.InterestDto> create(
        JwtAuthenticationToken auth,
        @Valid @RequestBody InterestDtos.CreateRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId(auth), req));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InterestDtos.InterestDto> upload(
        JwtAuthenticationToken auth,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().build();
        if (file.getSize() > MAX_UPLOAD_BYTES) return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        byte[] bytes = file.getBytes();
        if (!magicBytesMatch(bytes, contentType)) {
            // Client lied about content type — refuse rather than store a polyglot.
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        var dto = service.createWithUpload(userId(auth), file.getOriginalFilename(), bytes, contentType);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Sniffs the file header against the claimed MIME. PDFs start with {@code %PDF-} (5 bytes);
     * text/plain and text/markdown have no magic bytes — accept them as-is provided the body
     * is valid UTF-8.
     */
    private static boolean magicBytesMatch(byte[] bytes, String mime) {
        if ("application/pdf".equalsIgnoreCase(mime)) {
            if (bytes.length < PDF_MAGIC.length) return false;
            for (int i = 0; i < PDF_MAGIC.length; i++) {
                if (bytes[i] != PDF_MAGIC[i]) return false;
            }
            return true;
        }
        // text/plain or text/markdown — must decode as UTF-8.
        try {
            java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .decode(java.nio.ByteBuffer.wrap(bytes));
            return true;
        } catch (java.nio.charset.CharacterCodingException e) {
            return false;
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(JwtAuthenticationToken auth, @PathVariable("id") UUID id) {
        service.delete(userId(auth), id);
        return Map.of("ok", true);
    }

    private static UUID userId(JwtAuthenticationToken auth) {
        return UUID.fromString(auth.getToken().getSubject());
    }
}
