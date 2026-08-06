package com.aistudio.api.requirement.dto;

import java.time.Instant;
import java.util.UUID;

public record RequirementResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String improvedDescription,
        String userStories,
        String acceptanceCriteria,
        String status,
        String priority,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
