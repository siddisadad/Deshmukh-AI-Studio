package com.aistudio.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
