package com.aistudio.api.ops.dto;

import java.time.Instant;
import java.util.UUID;

public record ReleaseGateStatusResponse(
        boolean allowed,
        String imageTag,
        String reason,
        UUID latestPassRunId,
        Instant latestPassAt,
        int maxAgeHours
) {
}
