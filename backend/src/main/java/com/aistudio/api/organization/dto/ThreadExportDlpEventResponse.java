package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record ThreadExportDlpEventResponse(
        UUID id,
        UUID projectId,
        UUID conversationId,
        UUID exportId,
        UUID exportedByUserId,
        String matchCategories,
        boolean blocked,
        Instant siemExportedAt,
        Instant createdAt
) {
}
