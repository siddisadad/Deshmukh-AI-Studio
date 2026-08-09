package com.aistudio.api.codemetadata.dto;

import java.util.List;

public record CodeMetadataSummaryResponse(
        int fileCount,
        int maxFilesPerProject,
        List<CodeFileResponse> files
) {
}
