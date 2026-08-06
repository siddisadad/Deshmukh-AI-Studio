package com.aistudio.api.auth.dto;

import java.util.UUID;

public record TokenResponse(
        UserResponse user,
        OrganizationResponse organization,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public record UserResponse(UUID id, String email, String displayName, String theme) {
    }

    public record OrganizationResponse(UUID id, String name, String slug) {
    }
}
