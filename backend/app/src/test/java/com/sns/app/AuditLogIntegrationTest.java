package com.sns.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.app.support.IntegrationTestBase;
import com.sns.identity.repo.AuditLogRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Asserts the canonical auth lifecycle emits {@code audit_log} rows with the expected actions. */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuditLogRepository auditRepo;

    @Test
    void registerVerifyComplete_emitsAuditRows() throws Exception {
        String email = "audited@example.com";

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email))))
            .andExpect(status().isAccepted());

        mvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "tan", "123456"))))
            .andExpect(status().isOk());

        List<String> actions = auditRepo.findAll().stream().map(r -> r.getAction()).toList();
        assertThat(actions).contains("auth.register", "auth.verify");
    }
}
