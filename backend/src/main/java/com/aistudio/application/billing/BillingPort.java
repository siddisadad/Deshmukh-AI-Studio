package com.aistudio.application.billing;

import com.aistudio.domain.billing.PlanCode;
import java.util.List;
import java.util.UUID;

public interface BillingPort {
    String providerId();

    CheckoutSession createCheckoutSession(UUID organizationId, PlanCode planCode, String successUrl, String cancelUrl);

    String createCustomerPortalUrl(UUID organizationId, String returnUrl);

    List<InvoiceSummary> listInvoices(UUID organizationId, int limit);

    default void refreshSubscriptionItems(UUID organizationId, String externalSubscriptionId) {
        // no-op for mock
    }

    default MeteredUsageSyncResult reportMeteredUsage(
            UUID organizationId,
            long seatQuantity,
            long aiOverageQuantity,
            long timestampEpochSeconds
    ) {
        return MeteredUsageSyncResult.skipped("Metered usage sync not supported for provider " + providerId());
    }

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

    record MeteredUsageSyncResult(boolean synced, boolean skipped, String detail) {
        public static MeteredUsageSyncResult synced(String detail) {
            return new MeteredUsageSyncResult(true, false, detail);
        }

        public static MeteredUsageSyncResult skipped(String detail) {
            return new MeteredUsageSyncResult(false, true, detail);
        }

        public static MeteredUsageSyncResult failed(String detail) {
            return new MeteredUsageSyncResult(false, false, detail);
        }
    }
}
