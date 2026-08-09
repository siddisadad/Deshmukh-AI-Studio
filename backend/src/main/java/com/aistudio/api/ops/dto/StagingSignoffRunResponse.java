package com.aistudio.api.ops.dto;

import java.time.Instant;
import java.util.UUID;

public record StagingSignoffRunResponse(
        UUID id,
        String runType,
        String host,
        String environmentLabel,
        String imageTag,
        String overall,
        int passCount,
        int failCount,
        int skipCount,
        String s3Uri,
        Instant createdAt
) {
}
