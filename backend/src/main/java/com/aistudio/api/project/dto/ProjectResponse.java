package com.aistudio.api.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String projectKey,
        String description,
        String status,
        String role,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
