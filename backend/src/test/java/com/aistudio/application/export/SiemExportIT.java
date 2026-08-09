package com.aistudio.application.export;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SiemExportIT {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.billing.usage-sync-token", () -> "test-billing-usage-sync-token");
    }

    @Test
    void siemExportRequiresToken() throws Exception {
        mockMvc.perform(post("/api/v1/exports/siem/run"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void siemExportWithTokenReturnsNoPendingWhenEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/exports/siem/run")
                        .header("Authorization", "Bearer test-billing-usage-sync-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(0))
                .andExpect(jsonPath("$.messages[0]").value("No pending DLP events"));
    }
}
