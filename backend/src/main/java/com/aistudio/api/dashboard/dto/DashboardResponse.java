package com.aistudio.api.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        List<ProjectSummary> projects,
        List<ActivityItem> recentActivity
) {
    public record ProjectSummary(
            UUID id,
            String name,
            String projectKey,
            String status,
            long requirementCount,
            long openTaskCount,
            long doneTaskCount,
            Instant updatedAt
    ) {
    }

    public record ActivityItem(
            String action,
            String entityType,
            UUID entityId,
            Instant createdAt
    ) {
    }
}
