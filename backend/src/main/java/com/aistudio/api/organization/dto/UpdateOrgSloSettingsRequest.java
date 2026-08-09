package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateOrgSloSettingsRequest(
        @DecimalMin("0.9") @DecimalMax("0.999") double availabilityTarget,
        @DecimalMin("0.5") @DecimalMax("0.99") double latencyTarget,
        @Min(1) @Max(30) int latencyThresholdSeconds
) {
}
