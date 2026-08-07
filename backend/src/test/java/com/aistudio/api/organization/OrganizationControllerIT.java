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
class OrganizationControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void ownerCanAddExistingUserAsMember() throws Exception {
        JsonNode owner = register("owner" + System.currentTimeMillis() + "@example.com", "Owner");
        String ownerToken = owner.get("accessToken").asText();
        UUID orgId = UUID.fromString(owner.get("organization").get("id").asText());

        JsonNode invitee = register("invitee" + System.currentTimeMillis() + "@example.com", "Invitee");
        String inviteeEmail = invitee.get("user").get("email").asText();
        UUID inviteeId = UUID.fromString(invitee.get("user").get("id").asText());

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(inviteeEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(inviteeId.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(inviteeEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing%s@example.com","role":"MEMBER"}
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isNotFound());

        JsonNode other = register("other" + System.currentTimeMillis() + "@example.com", "Other");
        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"OWNER"}
                                """.formatted(other.get("user").get("email").asText())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private JsonNode register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"%s"}
                                """.formatted(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
