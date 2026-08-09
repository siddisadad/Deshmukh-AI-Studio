package com.aistudio.api.organization;

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
class OrgDlpConnectorControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void listConnectorsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + UUID.randomUUID() + "/dlp/connectors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanListEmptyConnectors() throws Exception {
        String email = "dlp" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "DLP Owner");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/dlp/connectors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void ownerCanCreateSiemConnector() throws Exception {
        String email = "dlpcreate" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "DLP Create");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/dlp/connectors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"siem-primary",
                                  "connectorType":"SIEM",
                                  "displayName":"Primary SIEM",
                                  "webhookUrl":"https://siem.example.com/events",
                                  "enabled":true,
                                  "blockOnMatch":false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("siem-primary"))
                .andExpect(jsonPath("$.connectorType").value("SIEM"));
    }

    private JsonNode register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123!","displayName":"%s"}
                                """.formatted(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
