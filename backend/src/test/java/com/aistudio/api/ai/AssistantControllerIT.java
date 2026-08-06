package com.aistudio.api.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AssistantControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/aistudio");
        registry.add("spring.datasource.username", () -> "aistudio");
        registry.add("spring.datasource.password", () -> "aistudio");
        registry.add("aistudio.security.jwt.secret", () -> "test-secret-key-must-be-at-least-32-bytes-long");
        registry.add("aistudio.ai.provider", () -> "mock");
    }

    @Test
    void listAssistantsAndChatWithSharedContext() throws Exception {
        mockMvc.perform(get("/api/v1/assistants")
                        .header("Authorization", "Bearer " + registerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistants.length()").value(4));

        JsonNode auth = register("chat" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Chat Proj","projectKey":"CH","description":"chat"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Auth","description":"Login and JWT"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/conversations/DEVELOPER/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Suggest a REST API for password reset"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.userMessage.sender").value("USER"))
                .andExpect(jsonPath("$.assistantMessage.sender").value("ASSISTANT"));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations/DEVELOPER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    private String registerToken() throws Exception {
        return register("assist" + System.currentTimeMillis() + "@example.com").get("accessToken").asText();
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Chat User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
