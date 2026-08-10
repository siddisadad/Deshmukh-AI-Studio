package com.aistudio.api.organization.dto;

import java.util.UUID;

public record OrgGitSyncClearIntervalProjectResponse(
        UUID projectId,
        Integer scheduledSyncIntervalMinutes,
        boolean updated
) {
}
