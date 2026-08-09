package com.aistudio.api.organization.dto;

import java.util.UUID;

public record OrgGitSyncRetryProjectResponse(
        UUID projectId,
        boolean enqueued,
        boolean skippedPending
) {
}
