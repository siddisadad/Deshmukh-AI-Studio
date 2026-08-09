package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.git")
public record GitProperties(
        String provider,
        String apiToken,
        String apiBaseUrl,
        String publicApiBaseUrl
) {
}
