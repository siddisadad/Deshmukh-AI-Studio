package com.aistudio.api.ops;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aistudio.support.IntegrationTestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpsJobQueueIT {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.billing.usage-sync-token", () -> "test-billing-usage-sync-token");
    }

    @Test
    void jobQueueRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/ops/jobs/queue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jobQueueReturnsMetricsWithToken() throws Exception {
        mockMvc.perform(get("/api/v1/ops/jobs/queue")
                        .header("Authorization", "Bearer test-billing-usage-sync-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").isNumber())
                .andExpect(jsonPath("$.suggestedReplicas").value(1))
                .andExpect(jsonPath("$.maxReplicas").value(6));
    }
}
