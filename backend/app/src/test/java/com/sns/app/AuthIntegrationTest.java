package com.sns.app;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void fullRegistrationFlow_issuesTokens_andAllowsProfileFetch() throws Exception {
        String email = "ada@example.com";

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email))))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.accepted").value(true));

        MvcResult verifyResult = mvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "tan", "123456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andReturn();

        JsonNode verifyBody = json.readTree(verifyResult.getResponse().getContentAsString());
        String verificationToken = verifyBody.get("verificationToken").asText();

        MvcResult completeResult = mvc.perform(post("/api/auth/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "verificationToken", verificationToken,
                    "firstName", "Ada",
                    "lastName", "Lovelace",
                    "password", "analytical-engine"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn();

        JsonNode tokens = json.readTree(completeResult.getResponse().getContentAsString());
        String access = tokens.get("accessToken").asText();

        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.firstName").value("Ada"));

        MvcResult loginResult = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "email", email,
                    "password", "analytical-engine"
                ))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode loginTokens = json.readTree(loginResult.getResponse().getContentAsString());
        assertThat(loginTokens.get("accessToken").asText()).isNotBlank();

        mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", loginTokens.get("refreshToken").asText()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void profileEndpoint_requiresAuth() throws Exception {
        mvc.perform(get("/api/profile"))
            .andExpect(status().isUnauthorized());
    }
}
