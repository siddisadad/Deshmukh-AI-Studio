package com.aistudio.api.project;

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
class ProjectControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void createListArchiveAndDenyCrossTenant() throws Exception {
        String emailA = "pa" + System.currentTimeMillis() + "@example.com";
        String emailB = "pb" + System.currentTimeMillis() + "@example.com";
        JsonNode userA = register(emailA, "User A");
        JsonNode userB = register(emailB, "User B");

        String tokenA = userA.get("accessToken").asText();
        String tokenB = userB.get("accessToken").asText();
        UUID orgA = UUID.fromString(userA.get("organization").get("id").asText());

        MvcResult createResult = mockMvc.perform(post("/api/v1/organizations/" + orgA + "/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Client Portal","projectKey":"CP","description":"Demo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectKey").value("CP"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn();

        UUID projectId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgA + "/projects")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Client Portal"));

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].projectKey").value("CP"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Login flow","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Implement auth","priority":"MEDIUM","status":"TODO"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].requirementCount").value(1))
                .andExpect(jsonPath("$.projects[0].openTaskCount").value(1))
                .andExpect(jsonPath("$.projects[0].doneTaskCount").value(0));

        mockMvc.perform(patch("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));

        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/archive")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    private JsonNode register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"%s"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
