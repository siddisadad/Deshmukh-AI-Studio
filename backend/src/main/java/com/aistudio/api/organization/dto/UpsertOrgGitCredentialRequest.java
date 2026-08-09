package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertOrgGitCredentialRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 512) String apiToken,
        @Size(max = 512) String apiBaseUrl,
        Boolean enabled
) {
}
