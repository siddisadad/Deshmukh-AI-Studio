package com.aistudio.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SsoCallbackRequest(
        @NotBlank String provider,
        @NotBlank String code,
        @NotBlank String state,
        String redirectUri
) {
}
