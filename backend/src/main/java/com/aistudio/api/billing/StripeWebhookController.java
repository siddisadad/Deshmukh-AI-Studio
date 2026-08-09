package com.aistudio.api.billing;

import com.aistudio.application.billing.BillingService;
import com.aistudio.api.billing.dto.StripeDunningRunResponse;
import com.aistudio.api.billing.dto.StripeMeteredSyncResponse;
import com.aistudio.api.billing.dto.StripeReconciliationResponse;
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

    @PostMapping("/api/v1/billing/stripe/sync-metered-usage")
    @Operation(summary = "Sync seat + AI overage usage to Stripe metered prices (BILLING_USAGE_SYNC_TOKEN)")
    public StripeMeteredSyncResponse syncMeteredUsage() {
        return billingService.syncStripeMeteredUsage();
    }

    @PostMapping("/api/v1/billing/stripe/reconcile")
    @Operation(summary = "Reconcile internal MTD revenue vs Stripe paid invoices (BILLING_USAGE_SYNC_TOKEN)")
    public StripeReconciliationResponse reconcileRevenue() {
        return billingService.reconcileStripeRevenue();
    }

    @PostMapping("/api/v1/billing/stripe/dunning/run")
    @Operation(summary = "Run scheduled dunning reminders for past-due subscriptions (BILLING_USAGE_SYNC_TOKEN)")
    public StripeDunningRunResponse runDunning() {
        return billingService.runStripeDunning();
    }
}
