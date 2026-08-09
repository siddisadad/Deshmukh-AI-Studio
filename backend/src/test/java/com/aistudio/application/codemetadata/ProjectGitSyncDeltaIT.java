package com.aistudio.application.codemetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aistudio.support.IntegrationTestProperties;
import java.util.List;
import java.util.Map;
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
class ProjectGitSyncDeltaIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProjectGitSyncService gitSyncService;
    @Autowired ProjectCodeMetadataService codeMetadataService;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void deltaSyncAppliesChangedAndRemovedPaths() throws Exception {
        JsonNode auth = register("git-delta" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID userId = UUID.fromString(auth.get("user").get("id").asText());
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Delta Proj","projectKey":"DL","description":"delta"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/delta-service","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        gitSyncService.syncProject(projectId);
        var before = codeMetadataService.summary(projectId, userId);
        assertThat(before.files()).extracting(f -> f.path()).contains("README.md");

        String payload = objectMapper.writeValueAsString(Map.of(
                "source", "webhook",
                "changedPaths", List.of("src/delta/Feature.java"),
                "removedPaths", List.of("README.md")
        ));
        gitSyncService.syncProject(projectId, payload);

        var after = codeMetadataService.summary(projectId, userId);
        assertThat(after.files()).extracting(f -> f.path()).doesNotContain("README.md");
        assertThat(after.files()).extracting(f -> f.path()).contains("src/delta/Feature.java");
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Delta User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
