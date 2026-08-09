package com.aistudio.api.codemetadata.dto;

import java.util.List;

public record GitSyncRunPageResponse(
        List<GitSyncRunResponse> items,
        int offset,
        int limit,
        long totalCount,
        boolean hasMore
) {
}
