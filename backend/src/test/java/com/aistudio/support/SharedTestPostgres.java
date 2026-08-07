package com.aistudio.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared pgvector Postgres for SpringBootTest ITs.
 * Prefers Testcontainers when Docker is available; otherwise falls back to env/local DB.
 */
public final class SharedTestPostgres {

    private static final Logger log = LoggerFactory.getLogger(SharedTestPostgres.class);

    private static final PostgreSQLContainer<?> CONTAINER;
    private static final boolean USING_CONTAINER;

    static {
        boolean docker = false;
        try {
            docker = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ex) {
            log.warn("Docker availability check failed; using external Postgres. {}", ex.toString());
        }

        if (docker) {
            DockerImageName image = DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres");
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>(image)
                    .withDatabaseName("aistudio")
                    .withUsername("aistudio")
                    .withPassword("aistudio");
            container.start();
            CONTAINER = container;
            USING_CONTAINER = true;
            log.info("Integration tests using Testcontainers Postgres at {}", container.getJdbcUrl());
        } else {
            CONTAINER = null;
            USING_CONTAINER = false;
            log.info("Integration tests using external Postgres (Docker unavailable)");
        }
    }

    private SharedTestPostgres() {
    }

    public static boolean usingContainer() {
        return USING_CONTAINER;
    }

    public static String jdbcUrl() {
        if (USING_CONTAINER) {
            return CONTAINER.getJdbcUrl();
        }
        return "jdbc:postgresql://"
                + env("DB_HOST", "localhost") + ":"
                + env("DB_PORT", "5432") + "/"
                + env("DB_NAME", "aistudio");
    }

    public static String username() {
        return USING_CONTAINER ? CONTAINER.getUsername() : env("DB_USER", "aistudio");
    }

    public static String password() {
        return USING_CONTAINER ? CONTAINER.getPassword() : env("DB_PASSWORD", "aistudio");
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
