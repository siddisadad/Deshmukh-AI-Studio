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
class OrgAiPolicySimulationGateIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.ai.policy-simulation-gate-enabled", () -> "true");
    }

    @Test
    void updateRequiresPassingSimulationWhenGateEnabled() throws Exception {
        JsonNode auth = register("aigate" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/ai-policy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerChain":"mock","dailyTokenBudget":40000,"deployRegion":"eu-west"}
                                """))
                .andExpect(status().isBadRequest());

        MvcResult simulateResult = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/ai-policy/simulate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerChain":"mock","dailyTokenBudget":40000,"deployRegion":"eu-west"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatePassed").value(true))
                .andReturn();

        String simulationId = objectMapper.readTree(simulateResult.getResponse().getContentAsString())
                .get("simulationId")
                .asText();

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/ai-policy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerChain":"mock",
                                  "dailyTokenBudget":40000,
                                  "deployRegion":"eu-west",
                                  "simulationId":"%s"
                                }
                                """.formatted(simulationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyTokenBudget").value(40000));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/ai-policy/simulations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appliedChangeId").isNotEmpty());
    }

    private JsonNode register(String email) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"Str0ngPass!","displayName":"Gate User"}
                                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }
}
