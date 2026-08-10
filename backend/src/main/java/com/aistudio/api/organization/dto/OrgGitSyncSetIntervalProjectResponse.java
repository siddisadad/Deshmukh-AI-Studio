package com.aistudio.api.organization.dto;

import java.util.UUID;

public record OrgGitSyncSetIntervalProjectResponse(
        UUID projectId,
        Integer scheduledSyncIntervalMinutes,
        boolean updated
) {
}
