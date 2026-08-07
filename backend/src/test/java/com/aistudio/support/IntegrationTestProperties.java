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
        // Touch SharedTestPostgres early so the singleton container starts once.
        SharedTestPostgres.jdbcUrl();

        registry.add("spring.datasource.url", SharedTestPostgres::jdbcUrl);
        registry.add("spring.datasource.username", SharedTestPostgres::username);
        registry.add("spring.datasource.password", SharedTestPostgres::password);
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
}
