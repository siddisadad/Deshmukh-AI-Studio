package com.aistudio.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SsoStartRequest(
        @NotBlank String provider,
        String redirectUri,
        String loginHint,
        UUID organizationId,
        String organizationSlug
) {
}
