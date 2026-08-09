package com.aistudio.api.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.aistudio.support.IntegrationTestProperties;
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

@SpringBootTest
@AutoConfigureMockMvc
class ThreadExportWatermarkDlpIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.ai.conversation.export-watermark-enabled", () -> "true");
        registry.add("aistudio.ai.conversation.export-watermark-notice", () -> "Test watermark notice");
        registry.add("aistudio.ai.conversation.export-dlp-enabled", () -> "true");
        registry.add("aistudio.ai.conversation.export-dlp-block-on-match", () -> "true");
    }

    @Test
    void exportIncludesWatermarkMetadata() throws Exception {
        JsonNode auth = register("watermark" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Watermark Proj","projectKey":"WM","description":"wm"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID threadId = createThread(token, projectId, "DEVELOPER", "Watermark thread");

        mockMvc.perform(post("/api/v1/conversations/" + threadId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Clean export content"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/conversations/" + threadId + "/export")
                        .param("format", "json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"exportId\"")))
                .andExpect(content().string(containsString("\"exportedByUserId\"")))
                .andExpect(content().string(containsString("Test watermark notice")));

        mockMvc.perform(get("/api/v1/conversations/" + threadId + "/export")
                        .param("format", "markdown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AI Studio export")));
    }

    @Test
    void exportBlockedWhenDlpMatches() throws Exception {
        JsonNode auth = register("dlpblock" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"DLP Proj","projectKey":"DL","description":"dlp"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID threadId = createThread(token, projectId, "DEVELOPER", "DLP thread");

        mockMvc.perform(post("/api/v1/conversations/" + threadId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"ssn 123-45-6789 on file"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/conversations/" + threadId + "/export")
                        .param("format", "json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(content().string(containsString("DLP")));
    }

    private JsonNode register(String email) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Export User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private UUID createThread(String token, UUID projectId, String role, String title) throws Exception {
        return UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/projects/" + projectId + "/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assistantRole":"%s","title":"%s"}
                                """.formatted(role, title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());
    }
}
