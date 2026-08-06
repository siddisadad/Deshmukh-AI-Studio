package com.aistudio.api.job.dto;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID projectId,
        String jobType,
        String status,
        String payload,
        String result,
        String errorMessage,
        int attempts,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
