package com.aistudio.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorShapeIT {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/aistudio");
        registry.add("spring.datasource.username", () -> "aistudio");
        registry.add("spring.datasource.password", () -> "aistudio");
        registry.add("aistudio.security.jwt.secret", () -> "test-secret-key-must-be-at-least-32-bytes-long");
        registry.add("aistudio.ai.provider", () -> "mock");
    }

    @Test
    void unauthorizedResponsesIncludeStructuredErrorAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("X-Request-Id", "api-error-shape-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "api-error-shape-test"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.requestId").value("api-error-shape-test"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401));
    }
}
