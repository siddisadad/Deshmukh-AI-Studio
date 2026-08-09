package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateOrgAiPolicyRequest(
        @Size(max = 255) String providerChain,
        Long dailyTokenBudget,
        @Size(max = 512) String modelMap,
        @Size(max = 64) String deployRegion,
        UUID simulationId
) {
}
