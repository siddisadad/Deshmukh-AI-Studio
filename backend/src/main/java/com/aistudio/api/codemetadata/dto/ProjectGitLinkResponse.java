package com.aistudio.api.codemetadata.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectGitLinkResponse(
        UUID id,
        UUID projectId,
        String provider,
        String repository,
        String branch,
        boolean enabled,
        boolean scheduledSyncEnabled,
        String webhookUrl,
        String webhookSecret,
        Instant lastSyncedAt,
        String lastSyncStatus,
        String lastSyncError,
        Integer scheduledSyncIntervalMinutes,
        List<String> pathIgnorePatterns,
        Instant updatedAt
) {
}
