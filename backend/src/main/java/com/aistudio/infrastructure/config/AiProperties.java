package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.ai")
public record AiProperties(
        String provider,
        OpenAi openai,
        Anthropic anthropic,
        Context context,
        RateLimit rateLimit
) {
    public record OpenAi(String apiKey, String model, String baseUrl) {
    }

    public record Anthropic(String apiKey, String model, String baseUrl) {
    }

    public record Context(int maxRequirements, int maxTasks, int maxMessages, int maxChars) {
    }

    public record RateLimit(int aiPerMinute) {
    }
}
