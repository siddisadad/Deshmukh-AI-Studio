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
        String canaryProviderChain,
        Integer canaryPercent,
        boolean canaryAutoPromoteEnabled,
        boolean canaryAutoAbortEnabled,
        String canaryHookWebhookUrl,
        int canaryMinSamples,
        int canaryAbortErrorRatePercent,
        int canaryPromoteMinSamples,
        int canaryPromoteMaxErrorRatePercent,
        OrgAiCanaryMetricsDto canaryMetrics,
        OrgAiPolicyChangeResponse pendingChange
) {
}
