package com.aistudio.api.requirement;

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
class RequirementControllerIT {

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
    void createRequirementAndRunBaAiActions() throws Exception {
        String email = "req" + System.currentTimeMillis() + "@example.com";
        JsonNode auth = register(email);
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        MvcResult projectResult = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Portal","projectKey":"PR","description":"Req demo"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID projectId = UUID.fromString(objectMapper.readTree(projectResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult reqResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Password reset","description":"User can reset password","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Password reset"))
                .andReturn();
        UUID requirementId = UUID.fromString(objectMapper.readTree(reqResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/requirements/" + requirementId + "/ai/improve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructions":"Focus on security"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantRole").value("BUSINESS_ANALYST"))
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.requirement.improvedDescription").isNotEmpty());

        mockMvc.perform(post("/api/v1/requirements/" + requirementId + "/ai/user-stories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirement.userStories").isNotEmpty());

        mockMvc.perform(post("/api/v1/requirements/" + requirementId + "/ai/acceptance-criteria")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirement.acceptanceCriteria").isNotEmpty());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].improvedDescription").isNotEmpty());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Req User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
