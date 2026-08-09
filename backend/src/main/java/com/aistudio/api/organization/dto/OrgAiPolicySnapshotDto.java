package com.aistudio.api.organization.dto;

public record OrgAiPolicySnapshotDto(
        String providerChain,
        Long dailyTokenBudget,
        long effectiveDailyTokenBudget,
        Long tokenBudgetRemaining,
        String modelMap,
        String deployRegion,
        String effectiveDeployRegion
) {
}
