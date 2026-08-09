package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgDlpConnectorResponse(
        UUID id,
        String slug,
        String connectorType,
        String displayName,
        String webhookUrl,
        boolean enabled,
        boolean blockOnMatch,
        String customPatternsJson,
        Instant createdAt,
        Instant updatedAt
) {
}
