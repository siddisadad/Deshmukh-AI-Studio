package com.aistudio.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SsoStartRequest(
        @NotBlank String provider,
        String redirectUri,
        String loginHint
) {
}
