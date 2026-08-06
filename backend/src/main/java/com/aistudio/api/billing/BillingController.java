package com.aistudio.api.billing;

import com.aistudio.api.billing.dto.BillingOverviewResponse;
import com.aistudio.api.billing.dto.ChangePlanRequest;
import com.aistudio.api.billing.dto.CheckoutRequest;
import com.aistudio.api.billing.dto.CheckoutResponse;
import com.aistudio.api.billing.dto.PlanResponse;
import com.aistudio.application.billing.BillingService;
import com.aistudio.domain.billing.PlanCode;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/api/v1/billing/plans")
    @Operation(summary = "List available plans")
    public List<PlanResponse> plans() {
        return billingService.listPlans();
    }

    @GetMapping("/api/v1/organizations/{orgId}/billing")
    @Operation(summary = "Billing overview for an organization")
    public BillingOverviewResponse overview(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return billingService.overview(orgId, user.getId());
    }

    @PostMapping("/api/v1/organizations/{orgId}/billing/checkout")
    @Operation(summary = "Start checkout for a paid plan (mock Stripe-shaped)")
    public CheckoutResponse checkout(
            @PathVariable UUID orgId,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return billingService.startCheckout(
                orgId,
                user.getId(),
                parsePlan(request.planCode()),
                request.successUrl(),
                request.cancelUrl()
        );
    }

    @PostMapping("/api/v1/organizations/{orgId}/billing/change-plan")
    @Operation(summary = "Change plan immediately (mock provider; Stripe webhook later)")
    public BillingOverviewResponse changePlan(
            @PathVariable UUID orgId,
            @Valid @RequestBody ChangePlanRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return billingService.changePlan(orgId, user.getId(), parsePlan(request.planCode()));
    }

    @PostMapping("/api/v1/organizations/{orgId}/billing/portal")
    @Operation(summary = "Create customer portal URL")
    public Map<String, String> portal(
            @PathVariable UUID orgId,
            @RequestParam(required = false) String returnUrl,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return Map.of("url", billingService.customerPortal(orgId, user.getId(), returnUrl));
    }

    private static PlanCode parsePlan(String value) {
        try {
            return PlanCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Invalid plan code");
        }
    }
}
