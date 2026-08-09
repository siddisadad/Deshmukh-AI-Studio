package com.aistudio.api.organization.dto;

import java.util.List;
import java.util.UUID;

public record OrgGitSyncOverviewResponse(
        UUID organizationId,
        int totalProjects,
        int linkedProjects,
        int enabledLinks,
        int scheduledSyncLinks,
        int failedLastSync,
        List<OrgGitSyncOverviewItemResponse> items
) {
}
