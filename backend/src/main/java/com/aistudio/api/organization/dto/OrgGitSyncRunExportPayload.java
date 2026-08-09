package com.aistudio.api.organization.dto;

import java.util.List;
import java.util.UUID;

public record OrgGitSyncRunExportPayload(
        UUID organizationId,
        long totalCount,
        long exportedCount,
        boolean truncated,
        List<OrgGitSyncRunItemResponse> items
) {
}
