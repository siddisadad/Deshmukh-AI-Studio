package com.aistudio.api.organization.dto;

public record OrgAiCanaryMetricsDto(
        long canarySuccessCount,
        long canaryFailureCount,
        long stableSuccessCount,
        long stableFailureCount
) {
}
