package com.aistudio.application.codemetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectGitLinkRepository;
import com.aistudio.support.IntegrationTestProperties;
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
class ProjectGitScheduledSyncIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProjectGitSyncService gitSyncService;
    @Autowired BackgroundJobRepository jobRepository;
    @Autowired ProjectGitLinkRepository gitLinkRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void enqueueScheduledSyncsSkipsProjectsWithPendingJobs() throws Exception {
        JsonNode auth = register("git-cron" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cron Proj","projectKey":"CR","description":"cron"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/cron-service","branch":"main","enabled":true}
                                """))
                .andExpect(status().isOk());

        int first = gitSyncService.enqueueScheduledSyncsForEnabledLinks();
        assertThat(first).isGreaterThanOrEqualTo(1);
        assertThat(jobRepository.countByProjectIdAndJobTypeAndStatus(
                projectId, JobType.CODE_METADATA_SYNC, com.aistudio.domain.job.JobStatus.PENDING
        )).isEqualTo(1);

        gitSyncService.enqueueScheduledSyncsForEnabledLinks();
        assertThat(jobRepository.countByProjectIdAndJobTypeAndStatus(
                projectId, JobType.CODE_METADATA_SYNC, com.aistudio.domain.job.JobStatus.PENDING
        )).isEqualTo(1);
    }

    @Test
    void enqueueScheduledSyncsSkipsLinksNotDueForPerProjectInterval() throws Exception {
        JsonNode auth = register("git-interval" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Interval Proj","projectKey":"IV","description":"interval"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/interval-service","branch":"main","enabled":true,"scheduledSyncIntervalMinutes":1440}
                                """))
                .andExpect(status().isOk());

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId).orElseThrow();
        link.setLastSyncedAt(Instant.now());
        gitLinkRepository.save(link);

        int enqueued = gitSyncService.enqueueScheduledSyncsForEnabledLinks();
        assertThat(jobRepository.countByProjectIdAndJobTypeAndStatus(
                projectId, JobType.CODE_METADATA_SYNC, com.aistudio.domain.job.JobStatus.PENDING
        )).isZero();
        assertThat(enqueued).isGreaterThanOrEqualTo(0);
    }

    @Test
    void enqueueScheduledSyncsSkipsLinksWithScheduledSyncDisabled() throws Exception {
        JsonNode auth = register("git-toggle" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Toggle Proj","projectKey":"TG","description":"toggle"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/toggle-service","branch":"main","enabled":true,"scheduledSyncEnabled":false}
                                """))
                .andExpect(status().isOk());

        gitSyncService.enqueueScheduledSyncsForEnabledLinks();
        assertThat(jobRepository.countByProjectIdAndJobTypeAndStatus(
                projectId, JobType.CODE_METADATA_SYNC, com.aistudio.domain.job.JobStatus.PENDING
        )).isZero();
    }

    @Test
    void enqueueScheduledSyncsRetriesFailedLinksDespitePerProjectInterval() throws Exception {
        JsonNode auth = register("git-failed-retry" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Failed Retry Proj","projectKey":"FR","description":"failed-retry"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/v1/projects/" + projectId + "/git-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"repository":"acme/failed-retry-service","branch":"main","enabled":true,"scheduledSyncIntervalMinutes":1440}
                                """))
                .andExpect(status().isOk());

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId).orElseThrow();
        link.setLastSyncedAt(Instant.now());
        link.setLastSyncStatus("failed");
        link.setLastSyncError("upstream timeout");
        gitLinkRepository.save(link);

        gitSyncService.enqueueScheduledSyncsForEnabledLinks();
        assertThat(jobRepository.countByProjectIdAndJobTypeAndStatus(
                projectId, JobType.CODE_METADATA_SYNC, com.aistudio.domain.job.JobStatus.PENDING
        )).isEqualTo(1);
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Cron User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
