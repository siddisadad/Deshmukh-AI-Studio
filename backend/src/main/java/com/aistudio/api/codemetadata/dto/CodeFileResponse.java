package com.aistudio.api.codemetadata.dto;

import java.time.Instant;
import java.util.UUID;

public record CodeFileResponse(
        UUID id,
        UUID projectId,
        String path,
        String language,
        String snippet,
        int sizeBytes,
        Instant updatedAt
) {
}
