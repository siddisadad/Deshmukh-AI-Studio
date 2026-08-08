package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.domain.common.AiProviderException;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries providers in order until one succeeds (generate + stream failover).
 */
public class RoutingAiProvider implements AiProviderPort {

    private static final Logger log = LoggerFactory.getLogger(RoutingAiProvider.class);

    private final AiProviderRegistry registry;
    private final List<String> chain;
    private final ThreadLocal<String> activeProviderId = new ThreadLocal<>();

    public RoutingAiProvider(AiProviderRegistry registry, List<String> chain) {
        this.registry = registry;
        this.chain = chain;
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request) {
        activeProviderId.remove();
        AiProviderException lastFailure = null;
        for (String providerId : chain) {
            AiProviderPort provider = registry.get(providerId);
            if (provider == null) {
                log.warn("Skipping AI provider {} — not configured", providerId);
                continue;
            }
            try {
                AiGenerationResult result = provider.generate(request);
                activeProviderId.set(provider.providerId());
                return result;
            } catch (AiProviderException ex) {
                lastFailure = ex;
                log.warn("AI provider {} failed: {}", providerId, ex.getMessage());
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AiProviderException("No configured providers in chain: " + String.join(",", chain));
    }

    @Override
    public AiGenerationResult stream(AiGenerationRequest request, Consumer<String> onDelta) {
        activeProviderId.remove();
        AiProviderException lastFailure = null;
        for (String providerId : chain) {
            AiProviderPort provider = registry.get(providerId);
            if (provider == null) {
                log.warn("Skipping AI provider {} — not configured", providerId);
                continue;
            }
            try {
                AiGenerationResult result = provider.stream(request, onDelta);
                activeProviderId.set(provider.providerId());
                return result;
            } catch (AiProviderException ex) {
                lastFailure = ex;
                log.warn("AI provider {} stream failed: {}", providerId, ex.getMessage());
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AiProviderException("No configured providers in chain: " + String.join(",", chain));
    }

    @Override
    public String providerId() {
        String active = activeProviderId.get();
        if (active != null) {
            return active;
        }
        return chain.isEmpty() ? "routing" : chain.getFirst();
    }
}
