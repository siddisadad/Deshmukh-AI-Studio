package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class OrgGitSyncFilterPresetControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void orgMemberCanSaveListAndDeleteGitSyncFilterPresets() throws Exception {
        JsonNode user = register("org-filter-preset" + System.currentTimeMillis() + "@example.com");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        MvcResult createResult = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "overview",
                                  "label": "My failed enabled",
                                  "filters": {
                                    "linked": "linked",
                                    "enabled": "enabled",
                                    "scheduled": "all",
                                    "interval": "all",
                                    "provider": "github",
                                    "status": "failed"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("overview"))
                .andExpect(jsonPath("$.label").value("My failed enabled"))
                .andExpect(jsonPath("$.filters.status").value("failed"))
                .andReturn();

        UUID presetId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(presetId.toString()));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "runs",
                                  "label": "Failed manual",
                                  "filters": {
                                    "source": "manual",
                                    "status": "failed",
                                    "project": "all"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("runs"));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "overview",
                                  "label": "Duplicate filters",
                                  "filters": {
                                    "linked": "linked",
                                    "enabled": "enabled",
                                    "scheduled": "all",
                                    "interval": "all",
                                    "provider": "github",
                                    "status": "failed"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/organizations/" + orgId + "/git-sync-filter-presets/" + presetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].scope").value("runs"));
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123!",
                                  "displayName": "Preset User",
                                  "organizationName": "Preset Org"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
