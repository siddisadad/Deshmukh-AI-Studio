package com.aistudio.api.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aistudio.support.IntegrationTestProperties;
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
class OpsReleaseGateIT {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.billing.usage-sync-token", () -> "test-billing-usage-sync-token");
    }

    @Test
    void releaseGateRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/ops/release-gate").param("imageTag", "v0.2.54-beta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitAndEvaluateReleaseGate() throws Exception {
        mockMvc.perform(post("/api/v1/ops/staging-signoff/submit")
                        .header("Authorization", "Bearer test-billing-usage-sync-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportJson": "{\\"host\\":\\"https://staging.example.com\\",\\"imageTag\\":\\"v0.2.54-beta\\",\\"summary\\":{\\"pass\\":1,\\"fail\\":0,\\"skip\\":0,\\"overall\\":\\"pass\\"}}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overall").value("pass"))
                .andExpect(jsonPath("$.imageTag").value("v0.2.54-beta"));

        mockMvc.perform(get("/api/v1/ops/release-gate")
                        .param("imageTag", "v0.2.54-beta")
                        .header("Authorization", "Bearer test-billing-usage-sync-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.imageTag").value("v0.2.54-beta"));

        mockMvc.perform(get("/api/v1/ops/release-gate")
                        .param("imageTag", "v0.2.99-beta")
                        .header("Authorization", "Bearer test-billing-usage-sync-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
    }
}
