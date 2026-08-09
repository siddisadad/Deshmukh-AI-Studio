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
        long activeMemberCount,
        int maxSeats,
        int aiActionsUsedToday,
        int aiActionsOverageToday,
        int maxAiActionsPerDay,
        long aiTokensUsedToday,
        long effectiveDailyTokenBudget,
        int periodOverageActions,
        int estimatedSeatCentsMonthly,
        int estimatedOverageCentsThisPeriod,
        String aiProviderChain
) {
}
