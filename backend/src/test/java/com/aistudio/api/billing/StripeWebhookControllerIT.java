package com.aistudio.api.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
class StripeWebhookControllerIT {

    private static final String WEBHOOK_SECRET = "whsec_test_stripe_webhook_secret_32chars";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        IntegrationTestProperties.register(registry);
        registry.add("aistudio.billing.stripe.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("aistudio.billing.stripe.pro-price-id", () -> "price_pro_test");
        registry.add("aistudio.billing.stripe.team-price-id", () -> "price_team_test");
    }

    @Test
    void checkoutSessionCompletedUpgradesPlan() throws Exception {
        String email = "stripe" + System.currentTimeMillis() + "@example.com";
        JsonNode user = register(email, "Stripe User");
        String token = user.get("accessToken").asText();
        UUID orgId = UUID.fromString(user.get("organization").get("id").asText());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/billing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("FREE"));

        String payload = """
                {
                  "id": "evt_test_webhook",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_webhook",
                      "object": "checkout.session",
                      "client_reference_id": "%s",
                      "customer": "cus_test_webhook",
                      "subscription": "sub_test_webhook",
                      "metadata": {
                        "organizationId": "%s",
                        "planCode": "PRO"
                      }
                    }
                  }
                }
                """.formatted(orgId, orgId);

        String signature = signStripePayload(payload, WEBHOOK_SECRET);

        mockMvc.perform(post("/api/v1/billing/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/organizations/" + orgId + "/billing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.code").value("PRO"))
                .andExpect(jsonPath("$.externalCustomerId").value("cus_test_webhook"))
                .andExpect(jsonPath("$.externalSubscriptionId").value("sub_test_webhook"));
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/v1/billing/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "bad")
                        .content("{\"id\":\"evt_bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_ERROR"));
    }

    private static String signStripePayload(String payload, String secret) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signed = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hex = HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + hex;
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
