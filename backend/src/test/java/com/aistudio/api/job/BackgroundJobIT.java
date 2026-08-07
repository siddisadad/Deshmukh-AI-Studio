package com.aistudio.api.job;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class BackgroundJobIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired com.aistudio.application.job.BackgroundJobWorker worker;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void enqueueKnowledgeReindexAndProcess() throws Exception {
        JsonNode auth = register("jobs" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());
        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jobs Proj","projectKey":"JB","description":"jobs"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Billing","description":"Invoice generation"}
                                """))
                .andExpect(status().isCreated());

        MvcResult enqueue = mockMvc.perform(post("/api/v1/projects/" + projectId + "/knowledge/reindex/async")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobType").value("KNOWLEDGE_REINDEX"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        UUID jobId = UUID.fromString(objectMapper.readTree(enqueue.getResponse().getContentAsString()).get("id").asText());

        worker.processOne(jobId);

        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.containsString("chunkCount")));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/jobs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(jobId.toString()));

        UUID documentId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/projects/" + projectId + "/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Guide","docType":"README","contentMd":""}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        MvcResult docJob = mockMvc.perform(post("/api/v1/documents/" + documentId + "/ai/generate/async")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"instructions":"Keep it short"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobType").value("DOCUMENT_GENERATE"))
                .andReturn();
        UUID docJobId = UUID.fromString(objectMapper.readTree(docJob.getResponse().getContentAsString()).get("id").asText());
        worker.processOne(docJobId);

        JsonNode finished = objectMapper.readTree(mockMvc.perform(get("/api/v1/jobs/" + docJobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andReturn().getResponse().getContentAsString());
        Assertions.assertTrue(finished.get("result").asText().contains("documentId"));
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Jobs User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
