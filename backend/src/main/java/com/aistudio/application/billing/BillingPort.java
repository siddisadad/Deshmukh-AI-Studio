package com.aistudio.application.billing;

import com.aistudio.domain.billing.PlanCode;
import java.util.List;
import java.util.UUID;

public interface BillingPort {
    String providerId();

    CheckoutSession createCheckoutSession(UUID organizationId, PlanCode planCode, String successUrl, String cancelUrl);

    String createCustomerPortalUrl(UUID organizationId, String returnUrl);

    List<InvoiceSummary> listInvoices(UUID organizationId, int limit);

    record CheckoutSession(String sessionId, String checkoutUrl) {
    }

    record InvoiceSummary(
            String id,
            String number,
            String status,
            long amountDueCents,
            String currency,
            Long createdAtEpochSeconds,
            String hostedInvoiceUrl,
            String invoicePdfUrl
    ) {
    }
}
