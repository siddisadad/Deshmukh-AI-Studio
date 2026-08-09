package com.aistudio.api.organization.dto;

import java.util.List;
import java.util.UUID;

public record OrgAiPolicySimulationResponse(
        UUID simulationId,
        OrgAiPolicySnapshotDto current,
        OrgAiPolicySnapshotDto simulated,
        List<String> currentEffectiveProviderChain,
        List<String> simulatedEffectiveProviderChain,
        List<String> missingProviders,
        boolean gatePassed,
        boolean wouldRequireApproval
) {
}
