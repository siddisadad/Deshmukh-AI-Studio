package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.visibility").value("private"))
                .andReturn();

        UUID presetId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(presetId.toString()))
                .andExpect(jsonPath("$[0].count").value(0))
                .andExpect(jsonPath("$[0].visibility").value("private"));

        mockMvc.perform(patch("/api/v1/organizations/" + orgId + "/git-sync-filter-presets/" + presetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label": "Renamed failed enabled"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(presetId.toString()))
                .andExpect(jsonPath("$.label").value("Renamed failed enabled"))
                .andExpect(jsonPath("$.filters.status").value("failed"));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Renamed failed enabled"));

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

    @Test
    void ownerCanShareOrgFilterPresetsWithMembers() throws Exception {
        JsonNode owner = register("preset-owner" + System.currentTimeMillis() + "@example.com", "Preset Owner");
        String ownerToken = owner.get("accessToken").asText();
        UUID orgId = UUID.fromString(owner.get("organization").get("id").asText());
        UUID ownerId = UUID.fromString(owner.get("user").get("id").asText());

        JsonNode member = register("preset-member" + System.currentTimeMillis() + "@example.com", "Preset Member");
        String memberEmail = member.get("user").get("email").asText();
        String memberToken = member.get("accessToken").asText();

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(memberEmail)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "overview",
                                  "label": "Member private",
                                  "visibility": "org",
                                  "filters": {
                                    "linked": "linked",
                                    "enabled": "all",
                                    "scheduled": "all",
                                    "interval": "all",
                                    "provider": "all",
                                    "status": "all"
                                  }
                                }
                                """))
                .andExpect(status().isForbidden());

        MvcResult orgPresetResult = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "overview",
                                  "label": "Org failed GitHub",
                                  "visibility": "org",
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
                .andExpect(jsonPath("$.visibility").value("org"))
                .andExpect(jsonPath("$.createdByUserId").value(ownerId.toString()))
                .andExpect(jsonPath("$.createdByDisplayName").value("Preset Owner"))
                .andReturn();

        UUID orgPresetId = UUID.fromString(
                objectMapper.readTree(orgPresetResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/git-sync-filter-presets")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orgPresetId.toString()))
                .andExpect(jsonPath("$[0].visibility").value("org"))
                .andExpect(jsonPath("$[0].label").value("Org failed GitHub"));

        mockMvc.perform(delete("/api/v1/organizations/" + orgId + "/git-sync-filter-presets/" + orgPresetId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/organizations/" + orgId + "/git-sync-filter-presets/" + orgPresetId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    private JsonNode register(String email) throws Exception {
        return register(email, "Preset User");
    }

    private JsonNode register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123!",
                                  "displayName": "%s",
                                  "organizationName": "Preset Org"
                                }
                                """.formatted(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
