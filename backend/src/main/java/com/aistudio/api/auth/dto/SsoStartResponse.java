package com.aistudio.api.auth.dto;

public record SsoStartResponse(
        String provider,
        String authorizationUrl,
        String state
) {
}
