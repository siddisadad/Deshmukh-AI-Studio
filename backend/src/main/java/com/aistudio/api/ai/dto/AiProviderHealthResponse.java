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
            String probeStatus,
            Instant probedAt
    ) {
    }
}
