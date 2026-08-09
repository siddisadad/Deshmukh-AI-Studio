package com.aistudio.api.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aistudio.support.IntegrationTestProperties;
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
class OrgSsoIdpControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void listRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + UUID.randomUUID() + "/sso/idps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanListEmptyIdps() throws Exception {
        String email = "sso" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "SSO Owner");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/sso/idps")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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
