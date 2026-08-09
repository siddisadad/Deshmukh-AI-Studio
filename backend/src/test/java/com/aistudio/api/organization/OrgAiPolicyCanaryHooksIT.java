package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aistudio.infrastructure.persistence.entity.OrgAiCanaryOutcomeEntity;
import com.aistudio.infrastructure.persistence.repository.OrgAiCanaryOutcomeRepository;
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
class OrgAiPolicyCanaryHooksIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OrgAiCanaryOutcomeRepository outcomeRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void evaluateAutoAbortsCanaryWhenErrorRateExceedsThreshold() throws Exception {
        JsonNode auth = register("canaryhooks" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/ai-policy/canary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerChain":"mock","percent":50}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/ai-policy/canary/hooks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "autoPromoteEnabled": false,
                                  "autoAbortEnabled": true,
                                  "hookWebhookUrl": null,
                                  "minSamples": 5,
                                  "abortErrorRatePercent": 50,
                                  "promoteMinSamples": 50,
                                  "promoteMaxErrorRatePercent": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canaryAutoAbortEnabled").value(true));

        OrgAiCanaryOutcomeEntity metrics = new OrgAiCanaryOutcomeEntity();
        metrics.setOrganizationId(orgId);
        metrics.setCanarySuccessCount(2L);
        metrics.setCanaryFailureCount(3L);
        outcomeRepository.save(metrics);

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/ai-policy/canary/evaluate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("ABORTED"))
                .andExpect(jsonPath("$.metrics.canarySuccessCount").value(0))
                .andExpect(jsonPath("$.metrics.canaryFailureCount").value(0));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/ai-policy")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canaryProviderChain").doesNotExist());
    }

    private JsonNode register(String email) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"Str0ngPass!","displayName":"Hooks User"}
                                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }
}
