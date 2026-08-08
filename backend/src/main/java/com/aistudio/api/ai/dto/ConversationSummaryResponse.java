package com.aistudio.api.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id,
        UUID projectId,
        String assistantRole,
        String title,
        Instant createdAt,
        Instant updatedAt,
        int messageCount,
        boolean shareEnabled,
        Instant shareExpiresAt,
        String visibility,
        boolean legalHold,
        Instant retentionExpiresAt
) {
}
