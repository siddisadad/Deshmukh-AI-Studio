package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String role,
        Instant createdAt
) {
}
