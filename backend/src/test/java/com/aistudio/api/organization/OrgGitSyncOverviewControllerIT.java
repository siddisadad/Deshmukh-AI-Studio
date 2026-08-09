package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectGitLinkRepository;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OrgGitSyncOverviewControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProjectGitLinkRepository gitLinkRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void orgMemberCanListGitSyncOverviewAcrossProjects() throws Exception {
        JsonNode user = register("org-sync-overview" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID linkedProjectId = createProject(token, orgId, "Linked Proj", "LK");
        UUID unlinkedProjectId = createProject(token, orgId, "Plain Proj", "PL");

        mockMvc.perform(put("/api/v1/projects/" + linkedProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/overview","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + linkedProjectId + "/git-link/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(orgId.toString()))
                .andExpect(jsonPath("$.totalProjects").value(2))
                .andExpect(jsonPath("$.linkedProjects").value(1))
                .andExpect(jsonPath("$.enabledLinks").value(1))
                .andExpect(jsonPath("$.failedLastSync").value(0))
                .andExpect(jsonPath("$.items[?(@.projectId == '" + linkedProjectId + "')].linked").value(true))
                .andExpect(jsonPath("$.items[?(@.projectId == '" + linkedProjectId + "')].lastSyncStatus").value("success"))
                .andExpect(jsonPath("$.items[?(@.projectId == '" + unlinkedProjectId + "')].linked").value(false));
    }

    @Test
    void overviewFiltersByLinkedProviderAndLastSyncStatus() throws Exception {
        JsonNode user = register("org-sync-filter" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID githubProjectId = createProject(token, orgId, "GitHub Proj", "GH");
        UUID gitlabProjectId = createProject(token, orgId, "GitLab Proj", "GL");
        UUID unlinkedProjectId = createProject(token, orgId, "Plain Proj", "PL");

        mockMvc.perform(put("/api/v1/projects/" + githubProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/gh","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + gitlabProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"gitlab","repository":"acme/gl","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + githubProjectId + "/git-link/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?linked=true&provider=github&lastSyncStatus=success")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(3))
                .andExpect(jsonPath("$.linkedProjects").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(githubProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?linked=false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(unlinkedProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?lastSyncStatus=never")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void orgOwnerCanRetryFailedGitSyncs() throws Exception {
        JsonNode user = register("org-retry-failed" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID failedProjectId = createProject(token, orgId, "Failed Proj", "FD");
        UUID okProjectId = createProject(token, orgId, "Ok Proj", "OK");

        mockMvc.perform(put("/api/v1/projects/" + failedProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/failed","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + okProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/ok","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        ProjectGitLinkEntity failedLink = gitLinkRepository.findByProjectId(failedProjectId).orElseThrow();
        failedLink.setLastSyncStatus("failed");
        failedLink.setLastSyncError("mock failure");
        gitLinkRepository.save(failedLink);

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/retry-failed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targeted").value(1))
                .andExpect(jsonPath("$.enqueued").value(1))
                .andExpect(jsonPath("$.skippedPending").value(0))
                .andExpect(jsonPath("$.enqueuedProjectIds[0]").value(failedProjectId.toString()));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/retry-failed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targeted").value(1))
                .andExpect(jsonPath("$.enqueued").value(0))
                .andExpect(jsonPath("$.skippedPending").value(1));
    }

    private UUID createProject(String token, UUID orgId, String name, String key) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","projectKey":"%s","description":"overview"}
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
                                  "displayName":"Sync Overview Owner",
                                  "organizationName":"Sync Overview Org"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
