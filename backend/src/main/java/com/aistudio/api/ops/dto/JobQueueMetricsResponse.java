package com.aistudio.api.ops.dto;

public record JobQueueMetricsResponse(
        long pending,
        long running,
        long failed,
        int suggestedReplicas,
        int targetPendingPerReplica,
        int maxReplicas
) {
}
