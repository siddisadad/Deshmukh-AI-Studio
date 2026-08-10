package com.aistudio.api.organization.dto;

import java.util.List;
import java.util.UUID;

public record OrgGitSyncDisableScheduledResponse(
        int targeted,
        int updated,
        List<UUID> updatedProjectIds
) {
}
