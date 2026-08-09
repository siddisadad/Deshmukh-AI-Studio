package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@SpringBootTest
@AutoConfigureMockMvc
class OrgAiPolicyCanaryIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void ownerCanStartPromoteAndAbortCanary() throws Exception {
        JsonNode auth = register("aicanary" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/ai-policy/canary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerChain":"mock","percent":25}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canaryProviderChain").value("mock"))
                .andExpect(jsonPath("$.canaryPercent").value(25));

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/ai-policy/canary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerChain":"mock","percent":50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canaryPercent").value(50));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/organizations/" + orgId + "/ai-policy/canary/promote")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerChain").value("mock"))
                .andExpect(jsonPath("$.canaryProviderChain").doesNotExist());

        mockMvc.perform(delete("/api/v1/organizations/" + orgId + "/ai-policy/canary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private JsonNode register(String email) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"Str0ngPass!","displayName":"Canary User"}
                                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }
}
