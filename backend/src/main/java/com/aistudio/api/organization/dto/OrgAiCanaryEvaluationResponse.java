package com.aistudio.api.organization.dto;

public record OrgAiCanaryEvaluationResponse(
        String action,
        String reason,
        OrgAiCanaryMetricsDto metrics
) {
}
