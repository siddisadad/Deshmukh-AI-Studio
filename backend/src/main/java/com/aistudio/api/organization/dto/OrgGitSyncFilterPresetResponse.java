package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrgGitSyncFilterPresetResponse(
        UUID id,
        String scope,
        String label,
        Map<String, String> filters,
        long count,
        Instant createdAt
) {}
