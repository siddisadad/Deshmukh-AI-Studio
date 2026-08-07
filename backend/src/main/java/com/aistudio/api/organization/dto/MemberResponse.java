package com.aistudio.api.organization.dto;

import java.util.UUID;

public record MemberResponse(
        UUID userId,
        String email,
        String displayName,
        String role
) {
}
