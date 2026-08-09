package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrgAiPolicyRequest(
        @Size(max = 255) String providerChain,
        Long dailyTokenBudget
) {
}
