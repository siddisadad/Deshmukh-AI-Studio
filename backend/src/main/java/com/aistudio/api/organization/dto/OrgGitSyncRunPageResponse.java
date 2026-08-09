package com.aistudio.api.organization.dto;

import com.aistudio.api.codemetadata.dto.GitSyncRunResponse;
import java.util.List;

public record OrgGitSyncRunPageResponse(
        List<OrgGitSyncRunItemResponse> items,
        int offset,
        int limit,
        long totalCount,
        boolean hasMore
) {
}
