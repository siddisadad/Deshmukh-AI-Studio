package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateOrgAiCanaryHooksRequest(
        boolean autoPromoteEnabled,
        boolean autoAbortEnabled,
        @Size(max = 512) String hookWebhookUrl,
        @Min(1) @Max(10000) int minSamples,
        @Min(1) @Max(100) int abortErrorRatePercent,
        @Min(1) @Max(10000) int promoteMinSamples,
        @Min(0) @Max(100) int promoteMaxErrorRatePercent
) {
}
