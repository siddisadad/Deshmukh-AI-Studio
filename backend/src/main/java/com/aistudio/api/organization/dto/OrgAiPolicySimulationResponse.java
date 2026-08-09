package com.aistudio.api.organization.dto;

import java.util.List;

public record OrgAiPolicySimulationResponse(
        OrgAiPolicySnapshotDto current,
        OrgAiPolicySnapshotDto simulated,
        List<String> currentEffectiveProviderChain,
        List<String> simulatedEffectiveProviderChain,
        List<String> missingProviders,
        boolean wouldRequireApproval
) {
}
