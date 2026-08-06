package com.aistudio.api.context.dto;

import java.time.Instant;
import java.util.UUID;

public record ContextAssetResponse(
        UUID id,
        UUID projectId,
        String assetType,
        String title,
        String content,
        String metadata,
        Instant updatedAt
) {
}
