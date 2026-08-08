package com.aistudio.infrastructure.config;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.infrastructure.ai.AiProviderRegistry;
import com.aistudio.infrastructure.ai.RoutingAiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProviderConfiguration {

    @Bean
    AiProviderRegistry aiProviderRegistry(AiProperties properties, ObjectMapper objectMapper) {
        return new AiProviderRegistry(properties, objectMapper);
    }

    @Bean
    AiProviderPort aiProviderPort(AiProperties properties, AiProviderRegistry registry) {
        String provider = normalize(properties.provider());
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if ("routing".equals(provider)) {
            List<String> chain = parseList(properties.providerChain());
            if (chain.isEmpty()) {
                throw new IllegalStateException(
                        "aistudio.ai.provider=routing requires AI_PROVIDER_CHAIN (e.g. openai,anthropic,mock)"
                );
            }
            return new RoutingAiProvider(registry, chain);
        }
        List<String> chain = new ArrayList<>();
        chain.add(provider);
        chain.addAll(parseList(properties.providerFallbacks()));
        if (chain.size() > 1) {
            return new RoutingAiProvider(registry, chain);
        }
        return registry.require(provider);
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
