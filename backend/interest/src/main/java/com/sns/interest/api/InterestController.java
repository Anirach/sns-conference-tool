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
        byte[] bytes = file.getBytes();
        var dto = service.createWithUpload(userId(auth), file.getOriginalFilename(), bytes, file.getContentType());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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
