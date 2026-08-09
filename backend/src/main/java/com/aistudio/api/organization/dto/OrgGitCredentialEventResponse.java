package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgGitCredentialEventResponse(
        UUID id,
        String provider,
        String action,
        UUID actorUserId,
        String displayName,
        String apiBaseUrl,
        Instant createdAt
) {
}
