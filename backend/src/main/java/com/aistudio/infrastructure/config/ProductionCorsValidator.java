package com.aistudio.infrastructure.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionCorsValidator implements ApplicationRunner {

    private final CorsProperties corsProperties;

    public ProductionCorsValidator(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> origins = corsProperties.allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException(
                    "Production requires CORS_ORIGINS with at least one HTTPS app origin");
        }
        for (String origin : origins) {
            String lower = origin.toLowerCase();
            if (lower.contains("localhost") || lower.contains("127.0.0.1") || lower.startsWith("http://")) {
                throw new IllegalStateException(
                        "Production CORS origins must use HTTPS and must not include localhost: " + origin);
            }
        }
    }
}
