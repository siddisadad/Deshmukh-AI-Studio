package com.aistudio.application.billing;

import com.aistudio.domain.billing.PlanCode;
import java.util.UUID;

public interface BillingPort {
    String providerId();

    CheckoutSession createCheckoutSession(UUID organizationId, PlanCode planCode, String successUrl, String cancelUrl);

    String createCustomerPortalUrl(UUID organizationId, String returnUrl);

    record CheckoutSession(String sessionId, String checkoutUrl) {
    }
}
