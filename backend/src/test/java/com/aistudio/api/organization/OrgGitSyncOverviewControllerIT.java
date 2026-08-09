package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
