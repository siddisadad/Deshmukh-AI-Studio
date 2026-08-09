package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgGitSyncRunItemResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String projectKey,
        UUID gitLinkId,
        String source,
        String status,
        int fileCount,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
