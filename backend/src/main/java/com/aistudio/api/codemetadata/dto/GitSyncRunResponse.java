package com.aistudio.api.codemetadata.dto;

import java.time.Instant;
import java.util.UUID;

public record GitSyncRunResponse(
        UUID id,
        UUID projectId,
        UUID gitLinkId,
        String source,
        String status,
        int fileCount,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
