package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateOrgAiCanaryRequest(
        @Size(max = 255) String providerChain,
        @Min(1) @Max(100) int percent
) {
}
