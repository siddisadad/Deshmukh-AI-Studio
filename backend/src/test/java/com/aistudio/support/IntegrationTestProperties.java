package com.aistudio.support;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Shared dynamic properties for SpringBootTest ITs so the application context is cached
 * across classes and Hikari pools do not multiply until Postgres runs out of clients.
 */
public final class IntegrationTestProperties {

    private IntegrationTestProperties() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:postgresql://"
                        + env("DB_HOST", "localhost") + ":"
                        + env("DB_PORT", "5432") + "/"
                        + env("DB_NAME", "aistudio"));
        registry.add("spring.datasource.username", () -> env("DB_USER", "aistudio"));
        registry.add("spring.datasource.password", () -> env("DB_PASSWORD", "aistudio"));
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "4");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
        registry.add("aistudio.security.jwt.secret",
                () -> "test-secret-key-must-be-at-least-32-bytes-long");
        registry.add("aistudio.ai.provider", () -> "mock");
        registry.add("aistudio.ai.embedding.provider", () -> "mock");
        registry.add("aistudio.ai.rag.enabled", () -> "true");
        registry.add("aistudio.jobs.poll-interval-ms", () -> "60000");
        registry.add("aistudio.billing.app-base-url", () -> "http://localhost:5173");
        registry.add("aistudio.sso.enabled", () -> "true");
        registry.add("aistudio.sso.provider", () -> "mock");
        registry.add("aistudio.sso.app-base-url", () -> "http://localhost:5173");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
