package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.ai")
public record AiProperties(
        String provider,
        String providerChain,
        String providerFallbacks,
        OpenAi openai,
        Anthropic anthropic,
        Context context,
        RateLimit rateLimit,
        Embedding embedding,
        Rag rag,
        CircuitBreaker circuitBreaker
) {
    public record OpenAi(String apiKey, String model, String baseUrl) {
    }

    public record Anthropic(String apiKey, String model, String baseUrl) {
    }

    public record Context(int maxRequirements, int maxTasks, int maxMessages, int maxChars) {
    }

    public record RateLimit(int aiPerMinute) {
    }

    public record Embedding(String provider, String model, int dimensions) {
    }

    public record Rag(boolean enabled, int topK, int maxChars) {
    }

    public record CircuitBreaker(boolean enabled, int failureThreshold, int openSeconds) {
    }
}
