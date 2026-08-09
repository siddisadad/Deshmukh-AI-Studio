package com.aistudio.api.ai.dto;

import java.time.Instant;
import java.util.List;

public record AiProviderHealthResponse(List<ProviderHealthDto> providers) {

    public record ProviderHealthDto(
            String id,
            boolean configured,
            String circuitState,
            int failureCount,
            Instant circuitOpenUntil,
            Long averageLatencyMs,
            int latencySampleCount,
            int costTier,
            Integer dailyQuota,
            Integer quotaUsedToday,
            Integer quotaRemaining,
            boolean quotaExhausted,
            String probeStatus,
            Instant probedAt
    ) {
    }
}
