package com.aistudio.api.organization;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void overviewFiltersByEnabledState() throws Exception {
        JsonNode user = register("org-sync-enabled" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID enabledProjectId = createProject(token, orgId, "Enabled Proj", "EN");
        UUID disabledProjectId = createProject(token, orgId, "Disabled Proj", "DS");
        UUID unlinkedProjectId = createProject(token, orgId, "Plain Proj", "PL");

        mockMvc.perform(put("/api/v1/projects/" + enabledProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/en","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + disabledProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/dis","branch":"main","enabled":false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?enabled=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(3))
                .andExpect(jsonPath("$.linkedProjects").value(2))
                .andExpect(jsonPath("$.enabledLinks").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(enabledProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?enabled=false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(disabledProjectId.toString()));

        MvcResult csvResult = mockMvc.perform(get("/api/v1/organizations/" + orgId
                        + "/git-sync-overview/export?format=csv&linked=true&enabled=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String csv = csvResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(enabledProjectId.toString()));
        org.junit.jupiter.api.Assertions.assertFalse(csv.contains(disabledProjectId.toString()));
        org.junit.jupiter.api.Assertions.assertFalse(csv.contains(unlinkedProjectId.toString()));
    }

    @Test
    void overviewFiltersByScheduledSyncEnabled() throws Exception {
        JsonNode user = register("org-sync-scheduled" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID scheduledProjectId = createProject(token, orgId, "Scheduled Proj", "SC");
        UUID manualProjectId = createProject(token, orgId, "Manual Proj", "MN");
        UUID unlinkedProjectId = createProject(token, orgId, "Plain Proj", "PL");

        mockMvc.perform(put("/api/v1/projects/" + scheduledProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/sc","branch":"main","enabled":true,"scheduledSyncEnabled":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + manualProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/mn","branch":"main","enabled":true,"scheduledSyncEnabled":false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledSyncLinks").value(1));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?scheduledSyncEnabled=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledSyncLinks").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(scheduledProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?scheduledSyncEnabled=false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(manualProjectId.toString()));

        MvcResult csvResult = mockMvc.perform(get("/api/v1/organizations/" + orgId
                        + "/git-sync-overview/export?format=csv&linked=true&scheduledSyncEnabled=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String csv = csvResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(scheduledProjectId.toString()));
        org.junit.jupiter.api.Assertions.assertFalse(csv.contains(manualProjectId.toString()));
        org.junit.jupiter.api.Assertions.assertFalse(csv.contains(unlinkedProjectId.toString()));
    }

    @Test
    void overviewFiltersByCustomSyncInterval() throws Exception {
        JsonNode user = register("org-sync-interval" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID customIntervalProjectId = createProject(token, orgId, "Custom Interval", "CI");
        UUID defaultIntervalProjectId = createProject(token, orgId, "Default Interval", "DI");
        UUID unlinkedProjectId = createProject(token, orgId, "Plain Proj", "PL");

        mockMvc.perform(put("/api/v1/projects/" + customIntervalProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/custom","branch":"main","enabled":true,"scheduledSyncIntervalMinutes":1440}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + defaultIntervalProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/default","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customSyncIntervalLinks").value(1));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?customSyncInterval=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customSyncIntervalLinks").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].projectId").value(customIntervalProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview?customSyncInterval=false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.projectId == '" + defaultIntervalProjectId + "')].linked").value(true))
                .andExpect(jsonPath("$.items[?(@.projectId == '" + unlinkedProjectId + "')].linked").value(false));

        MvcResult csvResult = mockMvc.perform(get("/api/v1/organizations/" + orgId
                        + "/git-sync-overview/export?format=csv&linked=true&customSyncInterval=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String csv = csvResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(customIntervalProjectId.toString()));
        org.junit.jupiter.api.Assertions.assertFalse(csv.contains(defaultIntervalProjectId.toString()));
    }

    @Test
    void orgOwnerCanBulkEnableScheduledSync() throws Exception {
        JsonNode user = register("org-enable-scheduled" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID manualProjectId = createProject(token, orgId, "Manual Proj", "MN");
        UUID scheduledProjectId = createProject(token, orgId, "Scheduled Proj", "SC");

        mockMvc.perform(put("/api/v1/projects/" + manualProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/manual","branch":"main","enabled":true,"scheduledSyncEnabled":false}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + scheduledProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/scheduled","branch":"main","enabled":true,"scheduledSyncEnabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualSyncLinks").value(1))
                .andExpect(jsonPath("$.scheduledSyncLinks").value(1));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/enable-scheduled-sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targeted").value(1))
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.updatedProjectIds[0]").value(manualProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualSyncLinks").value(0))
                .andExpect(jsonPath("$.scheduledSyncLinks").value(2));

        ProjectGitLinkEntity manualLink = gitLinkRepository.findByProjectId(manualProjectId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(manualLink.isScheduledSyncEnabled());
    }

    @Test
    void orgOwnerCanBulkDisableScheduledSync() throws Exception {
        JsonNode user = register("org-disable-scheduled" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID scheduledProjectId = createProject(token, orgId, "Scheduled Proj", "SC");
        UUID manualProjectId = createProject(token, orgId, "Manual Proj", "MN");

        mockMvc.perform(put("/api/v1/projects/" + scheduledProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/sc-dis","branch":"main","enabled":true,"scheduledSyncEnabled":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + manualProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/mn-dis","branch":"main","enabled":true,"scheduledSyncEnabled":false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/disable-scheduled-sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targeted").value(1))
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.updatedProjectIds[0]").value(scheduledProjectId.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledSyncLinks").value(0))
                .andExpect(jsonPath("$.manualSyncLinks").value(2));

        ProjectGitLinkEntity scheduledLink = gitLinkRepository.findByProjectId(scheduledProjectId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(scheduledLink.isScheduledSyncEnabled());
    }

    @Test
    void orgOwnerCanToggleScheduledSyncForSingleProject() throws Exception {
        JsonNode user = register("org-scheduled-one" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID projectId = createProject(token, orgId, "Toggle Proj", "TG");

        mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/toggle","branch":"main","enabled":true,"scheduledSyncEnabled":false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/organizations/" + orgId
                        + "/git-sync-overview/enable-scheduled-project/" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.scheduledSyncEnabled").value(true))
                .andExpect(jsonPath("$.updated").value(true));

        mockMvc.perform(post("/api/v1/organizations/" + orgId
                        + "/git-sync-overview/enable-scheduled-project/" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(false));

        mockMvc.perform(post("/api/v1/organizations/" + orgId
                        + "/git-sync-overview/disable-scheduled-project/" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledSyncEnabled").value(false))
                .andExpect(jsonPath("$.updated").value(true));

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(link.isScheduledSyncEnabled());
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

    @Test
    void orgOwnerCanRetryFailedGitSyncForSingleProject() throws Exception {
        JsonNode user = register("org-retry-one" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID failedProjectId = createProject(token, orgId, "Failed Proj", "FD");
        UUID okProjectId = createProject(token, orgId, "Ok Proj", "OK");

        mockMvc.perform(put("/api/v1/projects/" + failedProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/failed-one","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/" + okProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/ok-one","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        ProjectGitLinkEntity failedLink = gitLinkRepository.findByProjectId(failedProjectId).orElseThrow();
        failedLink.setLastSyncStatus("failed");
        failedLink.setLastSyncError("mock failure");
        gitLinkRepository.save(failedLink);

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/retry-project/" + failedProjectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(failedProjectId.toString()))
                .andExpect(jsonPath("$.enqueued").value(true))
                .andExpect(jsonPath("$.skippedPending").value(false));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/retry-project/" + failedProjectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enqueued").value(false))
                .andExpect(jsonPath("$.skippedPending").value(true));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-overview/retry-project/" + okProjectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void orgMemberCanExportGitSyncOverviewAsCsvAndJson() throws Exception {
        JsonNode user = register("org-sync-export" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        UUID linkedProjectId = createProject(token, orgId, "Export Proj", "EX");

        mockMvc.perform(put("/api/v1/projects/" + linkedProjectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"github","repository":"acme/export","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview/export?format=csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(header().string("Content-Type", containsString("text/csv")));

        MvcResult csvResult = mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview/export?format=csv&linked=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String csv = csvResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("projectId,projectName,projectKey"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(linkedProjectId.toString()));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("acme/export"));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview/export?format=json")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".json")))
                .andExpect(header().string("Content-Type", containsString("application/json")));

        MvcResult jsonResult = mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-overview/export?format=json&provider=github")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode exportJson = objectMapper.readTree(jsonResult.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertEquals(orgId.toString(), exportJson.get("organizationId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, exportJson.get("items").size());
        org.junit.jupiter.api.Assertions.assertEquals("github", exportJson.get("items").get(0).get("provider").asText());
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
