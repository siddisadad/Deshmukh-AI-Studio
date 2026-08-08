package com.aistudio.api.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import com.aistudio.support.IntegrationTestProperties;
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
        IntegrationTestProperties.register(registry);
    }

    @Test
    void listAssistantsAndChatAcrossMultipleThreads() throws Exception {
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

        UUID thread1 = createThread(token, projectId, "DEVELOPER", "API design");
        UUID thread2 = createThread(token, projectId, "DEVELOPER", "Password reset");

        mockMvc.perform(post("/api/v1/conversations/" + thread1 + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Suggest a REST API for password reset"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.userMessage.sender").value("USER"))
                .andExpect(jsonPath("$.assistantMessage.sender").value("ASSISTANT"));

        mockMvc.perform(post("/api/v1/conversations/" + thread2 + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Outline auth middleware"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/conversations/" + thread1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .param("q", "middleware")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Password reset"));

        mockMvc.perform(delete("/api/v1/conversations/" + thread2)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/conversations")
                        .param("assistantRole", "DEVELOPER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void streamChatMessageReturnsSseEvents() throws Exception {
        JsonNode auth = register("stream" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Stream Proj","projectKey":"ST","description":"stream"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID conversationId = createThread(token, projectId, "DEVELOPER", null);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"content":"Say hello in one sentence"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult(60_000);
        String body = mvcResult.getResponse().getContentAsString();
        Assertions.assertTrue(body.contains("event:user"), body);
        Assertions.assertTrue(body.contains("event:delta"), body);
        Assertions.assertTrue(body.contains("event:done"), body);

        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    private UUID createThread(String token, UUID projectId, String role, String title) throws Exception {
        String payload = title == null
                ? "{\"assistantRole\":\"%s\"}".formatted(role)
                : "{\"assistantRole\":\"%s\",\"title\":\"%s\"}".formatted(role, title);
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
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
