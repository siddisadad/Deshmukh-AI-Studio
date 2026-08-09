package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgGitSyncOverviewItemResponse(
        UUID projectId,
        String projectName,
        String projectKey,
        boolean linked,
        UUID linkId,
        String provider,
        String repository,
        String branch,
        boolean enabled,
        boolean scheduledSyncEnabled,
        Instant lastSyncedAt,
        String lastSyncStatus,
        String lastSyncError,
        Integer scheduledSyncIntervalMinutes
) {
}
