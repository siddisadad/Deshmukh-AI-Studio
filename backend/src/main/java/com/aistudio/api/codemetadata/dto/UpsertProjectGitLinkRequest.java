package com.aistudio.api.codemetadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertProjectGitLinkRequest(
        @Size(max = 20) String provider,
        @NotBlank @Size(max = 200) String repository,
        @Size(max = 100) String branch,
        Boolean enabled,
        Boolean regenerateWebhookSecret
) {
}
