package com.aistudio.api.organization.dto;

public record OrgAiPolicyResponse(
        String providerChain,
        Long dailyTokenBudget,
        long effectiveDailyTokenBudget,
        long tokensUsedToday,
        Long tokenBudgetRemaining,
        String modelMap,
        String deployRegion,
        String effectiveDeployRegion,
        boolean changeApprovalRequired,
        boolean simulationGateEnabled,
        OrgAiPolicyChangeResponse pendingChange
) {
}
