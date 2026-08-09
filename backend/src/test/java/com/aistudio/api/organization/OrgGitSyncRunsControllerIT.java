package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aistudio.infrastructure.persistence.entity.ProjectGitSyncRunEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectGitSyncRunRepository;
import com.aistudio.support.IntegrationTestProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
class OrgGitSyncRunsControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProjectGitSyncRunRepository syncRunRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void orgMemberCanListGitSyncRunsAcrossProjects() throws Exception {
        JsonNode user = register("org-sync-runs" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID projectA = createProject(token, orgId, "Alpha Proj", "AL");
        UUID projectB = createProject(token, orgId, "Beta Proj", "BE");

        MvcResult linkAResult = mockMvc.perform(put("/api/v1/projects/" + projectA + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/alpha","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        UUID gitLinkA = UUID.fromString(
                objectMapper.readTree(linkAResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult linkBResult = mockMvc.perform(put("/api/v1/projects/" + projectB + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/beta","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        UUID gitLinkB = UUID.fromString(
                objectMapper.readTree(linkBResult.getResponse().getContentAsString()).get("id").asText());

        seedRun(projectA, gitLinkA, "manual", "success", 3);
        seedRun(projectB, gitLinkB, "scheduled", "failed", 0, "timeout");

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs?limit=10&offset=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].projectKey").value("AL"))
                .andExpect(jsonPath("$.items[0].source").value("manual"))
                .andExpect(jsonPath("$.items[1].projectKey").value("BE"))
                .andExpect(jsonPath("$.items[1].status").value("failed"));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs?status=failed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(projectB.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs?projectId=" + projectA)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].projectName").value("Alpha Proj"));
    }

    private void seedRun(
            UUID projectId,
            UUID gitLinkId,
            String source,
            String status,
            int fileCount
    ) {
        seedRun(projectId, gitLinkId, source, status, fileCount, null);
    }

    private void seedRun(
            UUID projectId,
            UUID gitLinkId,
            String source,
            String status,
            int fileCount,
            String errorMessage
    ) {
        Instant now = Instant.now();
        ProjectGitSyncRunEntity run = new ProjectGitSyncRunEntity();
        run.setProjectId(projectId);
        run.setGitLinkId(gitLinkId);
        run.setSource(source);
        run.setStatus(status);
        run.setFileCount(fileCount);
        run.setErrorMessage(errorMessage);
        run.setStartedAt(now.minusSeconds(30));
        run.setFinishedAt(now);
        syncRunRepository.save(run);
    }

    private UUID createProject(String token, UUID orgId, String name, String key) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","projectKey":"%s","description":"sync runs"}
                                """.formatted(name, key)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"Password123!",
                                  "displayName":"Sync Runs Owner",
                                  "organizationName":"Sync Runs Org"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
