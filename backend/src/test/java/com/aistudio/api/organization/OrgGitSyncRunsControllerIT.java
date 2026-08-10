package com.aistudio.api.organization;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

        Instant now = Instant.now();
        seedRun(projectA, gitLinkA, "manual", "success", 3, null, now.minusSeconds(10));
        seedRun(projectB, gitLinkB, "scheduled", "failed", 0, "timeout", now);

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs?limit=10&offset=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].projectKey").value("BE"))
                .andExpect(jsonPath("$.items[0].status").value("failed"))
                .andExpect(jsonPath("$.items[1].projectKey").value("AL"))
                .andExpect(jsonPath("$.items[1].source").value("manual"));

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

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs/filter-counts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presets.length()").value(7))
                .andExpect(jsonPath("$.presets[?(@.id == 'failed')].count[0]").value(1))
                .andExpect(jsonPath("$.presets[?(@.id == 'success')].count[0]").value(1))
                .andExpect(jsonPath("$.presets[?(@.id == 'manual')].count[0]").value(1))
                .andExpect(jsonPath("$.presets[?(@.id == 'scheduled')].count[0]").value(1))
                .andExpect(jsonPath("$.presets[?(@.id == 'failed-scheduled')].count[0]").value(1))
                .andExpect(jsonPath("$.presets[?(@.id == 'webhook')].count[0]").value(0));
    }

    @Test
    void orgMemberCanExportGitSyncRunsAsCsvAndJson() throws Exception {
        JsonNode user = register("org-sync-export" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID projectId = createProject(token, orgId, "Export Proj", "EX");

        MvcResult linkResult = mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/export","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        UUID gitLinkId = UUID.fromString(
                objectMapper.readTree(linkResult.getResponse().getContentAsString()).get("id").asText());

        Instant now = Instant.now();
        seedRun(projectId, gitLinkId, "manual", "success", 5, null, now);

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs/export?format=csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(header().string("Content-Type", containsString("text/csv")));

        MvcResult csvResult = mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs/export?format=csv&source=manual")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String csv = csvResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("projectId,projectName,projectKey"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(projectId.toString()));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("manual"));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs/export?format=json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".json")));

        MvcResult jsonResult = mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-runs/export?format=json&status=success")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode exportJson = objectMapper.readTree(jsonResult.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertEquals(orgId.toString(), exportJson.get("organizationId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, exportJson.get("exportedCount").asInt());
        org.junit.jupiter.api.Assertions.assertEquals("success", exportJson.get("items").get(0).get("status").asText());
    }

    private void seedRun(
            UUID projectId,
            UUID gitLinkId,
            String source,
            String status,
            int fileCount
    ) {
        seedRun(projectId, gitLinkId, source, status, fileCount, null, Instant.now());
    }

    private void seedRun(
            UUID projectId,
            UUID gitLinkId,
            String source,
            String status,
            int fileCount,
            String errorMessage,
            Instant finishedAt
    ) {
        ProjectGitSyncRunEntity run = new ProjectGitSyncRunEntity();
        run.setProjectId(projectId);
        run.setGitLinkId(gitLinkId);
        run.setSource(source);
        run.setStatus(status);
        run.setFileCount(fileCount);
        run.setErrorMessage(errorMessage);
        run.setStartedAt(finishedAt.minusSeconds(30));
        run.setFinishedAt(finishedAt);
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
