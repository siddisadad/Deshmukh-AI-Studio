package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgAiPolicyChangeResponse(
        UUID id,
        String status,
        UUID proposedByUserId,
        UUID reviewedByUserId,
        String providerChain,
        Long dailyTokenBudget,
        String modelMap,
        String deployRegion,
        String previousPolicy,
        Instant createdAt,
        Instant reviewedAt
) {
}
