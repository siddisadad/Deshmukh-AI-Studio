package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgGitCredentialResponse(
        UUID id,
        String provider,
        String displayName,
        boolean configured,
        String apiBaseUrl,
        boolean enabled,
        String credentialSource,
        Instant lastTestedAt,
        String lastTestStatus,
        String lastTestError,
        Instant createdAt,
        Instant updatedAt
) {
}
