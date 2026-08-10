package com.aistudio.api.organization.dto;

import java.util.UUID;

public record OrgGitSyncScheduledProjectResponse(
        UUID projectId,
        boolean scheduledSyncEnabled,
        boolean updated
) {
}
