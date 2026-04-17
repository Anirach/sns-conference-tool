package com.sns.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.app.support.IntegrationTestBase;
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
class ChatIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void twoUsers_chatHistoryRoundtrip() throws Exception {
        String accessAlice = register("alice2@example.com", "Alice", "Two");
        String accessBob = register("bob2@example.com", "Bob", "Two");
        String eventId = jdbc.queryForObject(
            "SELECT event_id::text FROM events WHERE qr_code_plaintext = ? LIMIT 1",
            String.class, "NEURIPS2026");

        // Both join
        mvc.perform(post("/api/events/join").header("Authorization", "Bearer " + accessAlice)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("eventCode", "NEURIPS2026"))));
        mvc.perform(post("/api/events/join").header("Authorization", "Bearer " + accessBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("eventCode", "NEURIPS2026"))));

        String bobId = jdbc.queryForObject("SELECT user_id::text FROM users WHERE email = ?", String.class, "bob2@example.com");

        // Alice sends
        mvc.perform(post("/api/chat/send").header("Authorization", "Bearer " + accessAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "eventId", eventId,
                    "toUserId", bobId,
                    "content", "greetings, fellow"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("greetings, fellow"));

        // Bob reads history
        String historyBody = mvc.perform(get("/api/chat/" + eventId + "/" + aliceId(jdbc))
                .header("Authorization", "Bearer " + accessBob))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode body = json.readTree(historyBody);
        org.assertj.core.api.Assertions.assertThat(body.get("messages").size()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(body.get("messages").get(0).get("content").asText())
            .isEqualTo("greetings, fellow");
    }

    private String aliceId(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT user_id::text FROM users WHERE email = ?", String.class, "alice2@example.com");
    }

    private String register(String email, String first, String last) throws Exception {
        mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email))));
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
                    "password", "password-1234"
                )))).andReturn().getResponse().getContentAsString()
        );
        return tokens.get("accessToken").asText();
    }
}
