package com.aistudio.api.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.aistudio.support.IntegrationTestProperties;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void createLabelTaskAndMoveOnBoard() throws Exception {
        JsonNode auth = register("task" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Board","projectKey":"KB","description":"Kanban"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID labelId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"auth","color":"#0F766E"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        MvcResult taskResult = mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Reset API","priority":"HIGH","labelIds":["%s"]}
                                """.formatted(labelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.labels[0].name").value("auth"))
                .andReturn();
        UUID taskId = UUID.fromString(objectMapper.readTree(taskResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/tasks/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"updates":[{"taskId":"%s","status":"REVIEW","sortOrder":0}]}
                                """.formatted(taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REVIEW"));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Reset API"));
    }

    @Test
    void linkRequirementThenClearAndDeleteTask() throws Exception {
        JsonNode auth = register("task-req" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        UUID projectId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Req Link","projectKey":"RL","description":"Link reqs"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID requirementId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Auth stories","description":"Login","priority":"HIGH","status":"DRAFT"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        UUID taskId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Implement login","priority":"HIGH","requirementId":"%s"}
                                """.formatted(requirementId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requirementId").value(requirementId.toString()))
                .andReturn().getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clearRequirementId":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementId").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"Task User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
