package com.aistudio.api.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aistudio.application.billing.BillingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import com.aistudio.support.IntegrationTestProperties;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BillingControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BillingService billingService;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
    }

    @Test
    void plansOverviewFreeLimitAndUpgrade() throws Exception {
        String email = "bill" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "Billing User");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        mockMvc.perform(get("/api/v1/billing/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='FREE')].maxProjects").value(org.hamcrest.Matchers.contains(3)));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/billing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("FREE"))
                .andExpect(jsonPath("$.maxProjects").value(3))
                .andExpect(jsonPath("$.maxSeats").value(3))
                .andExpect(jsonPath("$.maxAiActionsPerDay").value(50))
                .andExpect(jsonPath("$.activeMemberCount").value(1))
                .andExpect(jsonPath("$.activeProjectCount").value(0));

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Project %d","projectKey":"P%d","description":"limit"}
                                    """.formatted(i, i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Project 4","projectKey":"P4","description":"over"}
                                """))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLAN_LIMIT"));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/billing/change-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planCode":"PRO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("PRO"))
                .andExpect(jsonPath("$.maxProjects").value(25));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Project 4","projectKey":"P4","description":"after upgrade"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/billing/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planCode":"TEAM","successUrl":"http://localhost:5173/settings/billing","cancelUrl":"http://localhost:5173/settings/billing"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.checkoutUrl").exists());
    }

    @Test
    void usageHistoryAndInvoices() throws Exception {
        String email = "usage" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "Usage User");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        billingService.requireAndConsumeAiAction(orgId);
        billingService.requireAndConsumeAiAction(orgId);

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/billing/usage")
                        .param("days", "7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[6].actionCount").value(2))
                .andExpect(jsonPath("$[6].overageCount").value(0));

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/billing/invoices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void seatLimitAndAiOverageMetering() throws Exception {
        String ownerEmail = "seat" + System.currentTimeMillis() + "@example.com";
        JsonNode owner = register(ownerEmail, "Seat Owner");
        String token = owner.get("accessToken").asText();
        UUID orgId = UUID.fromString(owner.get("organization").get("id").asText());

        String member2 = "seat2" + System.currentTimeMillis() + "@example.com";
        String member3 = "seat3" + System.currentTimeMillis() + "@example.com";
        register(member2, "Member Two");
        register(member3, "Member Three");

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(member2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(member3)))
                .andExpect(status().isCreated());

        String member4 = "seat4" + System.currentTimeMillis() + "@example.com";
        register(member4, "Member Four");
        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","role":"MEMBER"}
                                """.formatted(member4)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLAN_LIMIT"));

        mockMvc.perform(post("/api/v1/organizations/" + orgId + "/billing/change-plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planCode":"PRO"}
                                """))
                .andExpect(status().isOk());

        for (int i = 0; i < 500; i++) {
            billingService.requireAndConsumeAiAction(orgId);
        }
        billingService.requireAndConsumeAiAction(orgId);

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/billing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiActionsOverageToday").value(1))
                .andExpect(jsonPath("$.periodOverageActions").value(1))
                .andExpect(jsonPath("$.estimatedOverageCentsThisPeriod").value(2));
    }

    private JsonNode register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Str0ngPass!","displayName":"%s"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
