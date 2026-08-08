package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.domain.common.AiProviderException;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries providers in order until one succeeds (generate + stream failover).
 * Optional adaptive routing reorders the chain by recent latency before each request.
 */
public class RoutingAiProvider implements AiProviderPort {

    private static final Logger log = LoggerFactory.getLogger(RoutingAiProvider.class);

    private final AiProviderRegistry registry;
    private final List<String> chain;
    private final AiProviderCircuitBreaker circuitBreaker;
    private final AiProviderLatencyTracker latencyTracker;
    private final boolean adaptiveRoutingEnabled;
    private final ThreadLocal<String> activeProviderId = new ThreadLocal<>();

    public RoutingAiProvider(
            AiProviderRegistry registry,
            List<String> chain,
            AiProviderCircuitBreaker circuitBreaker
    ) {
        this(registry, chain, circuitBreaker, null, false);
    }

    public RoutingAiProvider(
            AiProviderRegistry registry,
            List<String> chain,
            AiProviderCircuitBreaker circuitBreaker,
            AiProviderLatencyTracker latencyTracker,
            boolean adaptiveRoutingEnabled
    ) {
        this.registry = registry;
        this.chain = chain;
        this.circuitBreaker = circuitBreaker;
        this.latencyTracker = latencyTracker;
        this.adaptiveRoutingEnabled = adaptiveRoutingEnabled && latencyTracker != null;
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request) {
        activeProviderId.remove();
        AiProviderException lastFailure = null;
        for (String providerId : orderedChain()) {
            if (circuitBreaker.shouldSkip(providerId)) {
                log.warn("Skipping AI provider {} — circuit open", providerId);
                continue;
            }
            AiProviderPort provider = registry.get(providerId);
            if (provider == null) {
                log.warn("Skipping AI provider {} — not configured", providerId);
                continue;
            }
            long startNanos = System.nanoTime();
            try {
                AiGenerationResult result = provider.generate(request);
                recordSuccess(providerId, startNanos);
                activeProviderId.set(provider.providerId());
                return result;
            } catch (AiProviderException ex) {
                circuitBreaker.recordFailure(providerId);
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
        for (String providerId : orderedChain()) {
            if (circuitBreaker.shouldSkip(providerId)) {
                log.warn("Skipping AI provider {} — circuit open", providerId);
                continue;
            }
            AiProviderPort provider = registry.get(providerId);
            if (provider == null) {
                log.warn("Skipping AI provider {} — not configured", providerId);
                continue;
            }
            long startNanos = System.nanoTime();
            try {
                AiGenerationResult result = provider.stream(request, onDelta);
                recordSuccess(providerId, startNanos);
                activeProviderId.set(provider.providerId());
                return result;
            } catch (AiProviderException ex) {
                circuitBreaker.recordFailure(providerId);
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

    private List<String> orderedChain() {
        if (!adaptiveRoutingEnabled) {
            return chain;
        }
        List<String> ordered = latencyTracker.orderByLatency(chain);
        if (!ordered.equals(chain)) {
            log.debug("Adaptive routing order: {} (chain: {})", ordered, chain);
        }
        return ordered;
    }

    private void recordSuccess(String providerId, long startNanos) {
        circuitBreaker.recordSuccess(providerId);
        if (latencyTracker != null) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            latencyTracker.recordLatency(providerId, latencyMs);
        }
    }
}
