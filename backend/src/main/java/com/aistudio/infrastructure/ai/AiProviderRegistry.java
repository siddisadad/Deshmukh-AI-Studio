package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lazily registers configured AI providers (mock always; OpenAI/Anthropic when API keys set).
 */
public class AiProviderRegistry {

    private final Map<String, AiProviderPort> providers = new HashMap<>();

    /** Empty registry for tests; use {@link #register(String, AiProviderPort)} to add providers. */
    public AiProviderRegistry() {
    }

    public AiProviderRegistry(AiProperties properties, ObjectMapper objectMapper) {
        providers.put("mock", new MockAiProvider());
        if (hasApiKey(properties.openai() == null ? null : properties.openai().apiKey())) {
            providers.put("openai", new OpenAiProvider(properties, objectMapper));
        }
        if (hasApiKey(properties.anthropic() == null ? null : properties.anthropic().apiKey())) {
            providers.put("anthropic", new AnthropicProvider(properties, objectMapper));
        }
    }

    public AiProviderPort require(String providerId) {
        String id = normalize(providerId);
        AiProviderPort provider = providers.get(id);
        if (provider == null) {
            throw new IllegalStateException("AI provider not configured: " + providerId);
        }
        return provider;
    }

    public AiProviderPort get(String providerId) {
        return providers.get(normalize(providerId));
    }

    public void register(String providerId, AiProviderPort provider) {
        providers.put(normalize(providerId), provider);
    }

    public List<String> configuredProviderIds() {
        return List.copyOf(providers.keySet());
    }

    private static boolean hasApiKey(String apiKey) {
        return apiKey != null && !apiKey.isBlank();
    }

    private static String normalize(String providerId) {
        return providerId == null ? "" : providerId.trim().toLowerCase(Locale.ROOT);
    }
}
