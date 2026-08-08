package com.aistudio.application.job;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import com.aistudio.support.IntegrationTestProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class BackgroundJobHorizontalIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BackgroundJobClaimer claimer;
    @Autowired TransactionTemplate transactionTemplate;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void multipleWorkersClaimDistinctPendingJobs() throws Exception {
        JsonNode auth = register("horizontal" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = createProject(token, orgId);
        createRequirement(token, projectId);

        UUID jobA = enqueueReindex(token, projectId);
        UUID jobB = enqueueReindex(token, projectId);

        List<UUID> workerOneClaims = transactionTemplate.execute(status ->
                claimer.claimNext(1, "worker-one"));
        List<UUID> workerTwoClaims = transactionTemplate.execute(status ->
                claimer.claimNext(1, "worker-two"));

        Assertions.assertEquals(1, workerOneClaims.size());
        Assertions.assertEquals(1, workerTwoClaims.size());
        Assertions.assertNotEquals(workerOneClaims.get(0), workerTwoClaims.get(0));
        Assertions.assertTrue(List.of(jobA, jobB).contains(workerOneClaims.get(0)));
        Assertions.assertTrue(List.of(jobA, jobB).contains(workerTwoClaims.get(0)));

        // Third claim should find nothing left
        List<UUID> workerThreeClaims = transactionTemplate.execute(status ->
                claimer.claimNext(5, "worker-three"));
        Assertions.assertTrue(workerThreeClaims == null || workerThreeClaims.isEmpty());
    }

    private UUID createProject(String token, UUID orgId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Scale Proj","projectKey":"SC","description":"scale"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void createRequirement(String token, UUID projectId) throws Exception {
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Billing","description":"Invoice generation"}
                                """))
                .andExpect(status().isCreated());
    }

    private UUID enqueueReindex(String token, UUID projectId) throws Exception {
        MvcResult enqueue = mockMvc.perform(post("/api/v1/projects/" + projectId + "/knowledge/reindex/async")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(enqueue.getResponse().getContentAsString()).get("id").asText());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Scale User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
