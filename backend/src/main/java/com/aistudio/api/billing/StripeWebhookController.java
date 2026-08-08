package com.aistudio.api.billing;

import com.aistudio.application.billing.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Billing")
public class StripeWebhookController {

    private final BillingService billingService;

    public StripeWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/api/v1/billing/stripe/webhook")
    @Operation(summary = "Stripe webhook (signature verified; no JWT)")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @org.springframework.web.bind.annotation.RequestHeader("Stripe-Signature") String signature
    ) {
        billingService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
