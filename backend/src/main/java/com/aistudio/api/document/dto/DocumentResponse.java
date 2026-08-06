package com.aistudio.api.document.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID projectId,
        String title,
        String docType,
        String contentMd,
        Instant createdAt,
        Instant updatedAt
) {
}
