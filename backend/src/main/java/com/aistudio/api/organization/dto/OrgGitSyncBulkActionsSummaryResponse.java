package com.aistudio.api.organization.dto;

import java.util.UUID;

public record OrgGitSyncBulkActionsSummaryResponse(
        UUID organizationId,
        int filteredItems,
        int retryFailedTargeted,
        int retryFailedPendingSkipped,
        int enableScheduledTargeted,
        int disableScheduledTargeted,
        int clearIntervalTargeted,
        int setIntervalTargeted
) {
}
