package com.aistudio.api.task.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        UUID requirementId,
        String title,
        String description,
        String status,
        String priority,
        UUID assigneeId,
        int sortOrder,
        List<LabelResponse> labels,
        Instant createdAt,
        Instant updatedAt
) {
}
