package com.sns.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.app.support.IntegrationTestBase;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class EventAndMatchingIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void twoUsersJoinTheSameEvent_overlappingInterests_appearInEachOthersVicinity() throws Exception {
        String accessAlice = registerAndLogin("alice@example.com", "Alice", "Liddell");
        String accessBob = registerAndLogin("bob@example.com", "Bob", "Marley");

        // Both join the seeded demo event
        mvc.perform(post("/api/events/join").header("Authorization", "Bearer " + accessAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("eventCode", "NEURIPS2026"))))
            .andExpect(status().isOk());
        String eventId = jdbc.queryForObject(
            "SELECT event_id FROM events WHERE qr_code_plaintext = ? LIMIT 1",
            (rs, i) -> rs.getString("event_id"), "NEURIPS2026");

        mvc.perform(post("/api/events/join").header("Authorization", "Bearer " + accessBob)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("eventCode", "NEURIPS2026"))))
            .andExpect(status().isOk());

        // Submit overlapping interests
        String interestText = "We study graph neural networks attention transformers federated learning privacy";
        mvc.perform(post("/api/interests").header("Authorization", "Bearer " + accessAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("type", "TEXT", "content", interestText))))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/interests").header("Authorization", "Bearer " + accessBob)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("type", "TEXT", "content",
                    "transformers attention graph neural networks diffusion models")))).andExpect(status().isCreated());

        // Ingest locations 10m apart
        mvc.perform(post("/api/events/" + eventId + "/location")
                .header("Authorization", "Bearer " + accessAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":13.7,\"lon\":100.55,\"accuracyMeters\":5}"))
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/events/" + eventId + "/location")
                .header("Authorization", "Bearer " + accessBob)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":13.70009,\"lon\":100.55009,\"accuracyMeters\":5}"))
            .andExpect(status().isNoContent());

        // Matching runs via async event listener — poll until a non-zero match exists.
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM similarity_matches WHERE event_id = ?::uuid AND similarity > 0",
                Integer.class, eventId);
            assertThat(count).isGreaterThan(0);
        });

        // Alice sees Bob in vicinity
        mvc.perform(get("/api/events/" + eventId + "/vicinity?radius=100")
                .header("Authorization", "Bearer " + accessAlice))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.radius").value(100))
            .andExpect(jsonPath("$.matches.length()").value(1))
            .andExpect(jsonPath("$.matches[0].name").value("Bob Marley"))
            .andExpect(jsonPath("$.matches[0].similarity").isNumber());
    }

    private String registerAndLogin(String email, String first, String last) throws Exception {
        mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email)))).andExpect(status().isAccepted());

        String verificationToken = json.readTree(
            mvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "tan", "123456"))))
                .andReturn().getResponse().getContentAsString()
        ).get("verificationToken").asText();

        JsonNode tokens = json.readTree(
            mvc.perform(post("/api/auth/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "verificationToken", verificationToken,
                    "firstName", first,
                    "lastName", last,
                    "password", "solvethisequation"
                ))))
                .andReturn().getResponse().getContentAsString()
        );
        return tokens.get("accessToken").asText();
    }
}
