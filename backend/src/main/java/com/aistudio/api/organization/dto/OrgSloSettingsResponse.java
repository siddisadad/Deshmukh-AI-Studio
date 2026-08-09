package com.aistudio.api.organization.dto;

public record OrgSloSettingsResponse(
        double availabilityTarget,
        double latencyTarget,
        int latencyThresholdSeconds
) {
}
