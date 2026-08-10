package com.aistudio.api.organization.dto;

import java.util.Map;

public record CreateOrgGitSyncFilterPresetRequest(
        String scope,
        String label,
        Map<String, String> filters,
        String visibility
) {}
