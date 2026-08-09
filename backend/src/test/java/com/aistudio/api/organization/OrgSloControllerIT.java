package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class OrgSloControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void ownerCanReadAndUpdateOrgSloSettings() throws Exception {
        JsonNode auth = register("orgslo" + System.currentTimeMillis() + "@example.com");
        String token = auth.get("accessToken").asText();
        UUID orgId = UUID.fromString(auth.get("organization").get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/slo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityTarget").value(0.995))
                .andExpect(jsonPath("$.latencyTarget").value(0.95))
                .andExpect(jsonPath("$.latencyThresholdSeconds").value(2));

        mockMvc.perform(put("/api/v1/organizations/" + orgId + "/slo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityTarget": 0.99,
                                  "latencyTarget": 0.9,
                                  "latencyThresholdSeconds": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityTarget").value(0.99))
                .andExpect(jsonPath("$.latencyTarget").value(0.9))
                .andExpect(jsonPath("$.latencyThresholdSeconds").value(5));
    }

    private JsonNode register(String email) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"Str0ngPass!","displayName":"SLO User"}
                                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }
}
