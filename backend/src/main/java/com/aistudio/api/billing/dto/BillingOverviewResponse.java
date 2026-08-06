package com.aistudio.api.billing.dto;

import java.time.Instant;

public record BillingOverviewResponse(
        PlanResponse plan,
        String subscriptionStatus,
        String billingProvider,
        String externalCustomerId,
        String externalSubscriptionId,
        Instant currentPeriodEnd,
        long activeProjectCount,
        int maxProjects,
        int aiActionsUsedToday,
        int maxAiActionsPerDay
) {
}
