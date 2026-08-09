package com.aistudio.api.codemetadata.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpsertProjectGitLinkRequest(
        @Size(max = 20) String provider,
        @NotBlank @Size(max = 200) String repository,
        @Size(max = 100) String branch,
        Boolean enabled,
        Boolean scheduledSyncEnabled,
        Boolean regenerateWebhookSecret,
        @Min(15) @Max(10080) Integer scheduledSyncIntervalMinutes,
        Boolean clearScheduledSyncInterval,
        @Size(max = 50) List<@Size(max = 200) String> pathIgnorePatterns,
        Boolean clearPathIgnorePatterns,
        @Size(max = 50) List<@Size(max = 200) String> pathIncludePatterns,
        Boolean clearPathIncludePatterns
) {
}
