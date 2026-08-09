package com.aistudio.api.organization.dto;

import java.util.List;
import java.util.UUID;

public record OrgGitSyncRetryFailedResponse(
        int targeted,
        int enqueued,
        int skippedPending,
        List<UUID> enqueuedProjectIds
) {
}
