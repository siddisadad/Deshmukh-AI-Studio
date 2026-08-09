package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrgAiPolicySimulationRecordResponse(
        UUID id,
        UUID simulatedByUserId,
        String providerChain,
        Long dailyTokenBudget,
        String modelMap,
        String deployRegion,
        List<String> missingProviders,
        List<String> currentEffectiveProviderChain,
        List<String> simulatedEffectiveProviderChain,
        boolean gatePassed,
        UUID appliedChangeId,
        Instant createdAt
) {
}
