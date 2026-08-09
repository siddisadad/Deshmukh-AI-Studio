package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.application.ai.OrgAiRoutingContext;
import com.aistudio.application.ai.OrgAiRoutingPolicyService;
import com.aistudio.domain.common.AiProviderException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries providers in order until one succeeds (generate + stream failover).
 * Optional adaptive routing reorders by recent latency; cost-aware routing prefers cheaper tiers.
 * Provider quotas skip exhausted providers for the UTC day.
 */
public class RoutingAiProvider implements AiProviderPort {

    private static final Logger log = LoggerFactory.getLogger(RoutingAiProvider.class);

    private final AiProviderRegistry registry;
    private final List<String> chain;
    private final AiProviderCircuitBreaker circuitBreaker;
    private final AiProviderLatencyTracker latencyTracker;
    private final boolean adaptiveRoutingEnabled;
    private final AiProviderCostTierRegistry costTierRegistry;
    private final AiProviderQuotaTracker quotaTracker;
    private final boolean costAwareRoutingEnabled;
    private final OrgAiRoutingPolicyService routingPolicyService;
    private final ThreadLocal<String> activeProviderId = new ThreadLocal<>();

    public RoutingAiProvider(
            AiProviderRegistry registry,
            List<String> chain,
            AiProviderCircuitBreaker circuitBreaker
    ) {
        this(registry, chain, circuitBreaker, null, false, null, null, false, null);
    }

    public RoutingAiProvider(
            AiProviderRegistry registry,
            List<String> chain,
            AiProviderCircuitBreaker circuitBreaker,
            AiProviderLatencyTracker latencyTracker,
            boolean adaptiveRoutingEnabled
    ) {
        this(
                registry,
                chain,
                circuitBreaker,
                latencyTracker,
                adaptiveRoutingEnabled,
                null,
                null,
                false,
                null);
    }

    public RoutingAiProvider(
            AiProviderRegistry registry,
            List<String> chain,
            AiProviderCircuitBreaker circuitBreaker,
            AiProviderLatencyTracker latencyTracker,
            boolean adaptiveRoutingEnabled,
            AiProviderCostTierRegistry costTierRegistry,
            AiProviderQuotaTracker quotaTracker,
            boolean costAwareRoutingEnabled
    ) {
        this(
                registry,
                chain,
                circuitBreaker,
                latencyTracker,
                adaptiveRoutingEnabled,
                costTierRegistry,
                quotaTracker,
                costAwareRoutingEnabled,
                null);
    }

    public RoutingAiProvider(
            AiProviderRegistry registry,
            List<String> chain,
            AiProviderCircuitBreaker circuitBreaker,
            AiProviderLatencyTracker latencyTracker,
            boolean adaptiveRoutingEnabled,
            AiProviderCostTierRegistry costTierRegistry,
            AiProviderQuotaTracker quotaTracker,
            boolean costAwareRoutingEnabled,
            OrgAiRoutingPolicyService routingPolicyService
    ) {
        this.registry = registry;
        this.chain = chain;
        this.circuitBreaker = circuitBreaker;
        this.latencyTracker = latencyTracker;
        this.adaptiveRoutingEnabled = adaptiveRoutingEnabled && latencyTracker != null;
        this.costTierRegistry = costTierRegistry;
        this.quotaTracker = quotaTracker;
        this.costAwareRoutingEnabled = costAwareRoutingEnabled && costTierRegistry != null;
        this.routingPolicyService = routingPolicyService;
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request) {
        activeProviderId.remove();
        AiProviderException lastFailure = null;
        for (String providerId : orderedChain()) {
            if (shouldSkip(providerId)) {
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
            if (shouldSkip(providerId)) {
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

    private boolean shouldSkip(String providerId) {
        if (circuitBreaker.shouldSkip(providerId)) {
            log.warn("Skipping AI provider {} — circuit open", providerId);
            return true;
        }
        if (quotaTracker != null && quotaTracker.isQuotaExhausted(providerId)) {
            log.warn("Skipping AI provider {} — daily quota exhausted", providerId);
            return true;
        }
        return false;
    }

    private List<String> effectiveChain() {
        UUID orgId = OrgAiRoutingContext.organizationId();
        if (routingPolicyService != null && orgId != null) {
            var orgChain = routingPolicyService.resolveChain(orgId);
            if (orgChain.isPresent() && !orgChain.get().isEmpty()) {
                return orgChain.get();
            }
        }
        return chain;
    }

    private List<String> orderedChain() {
        List<String> base = effectiveChain();
        if (!adaptiveRoutingEnabled && !costAwareRoutingEnabled) {
            return base;
        }
        Map<String, Integer> originalIndex = new HashMap<>();
        for (int i = 0; i < base.size(); i++) {
            originalIndex.put(base.get(i), i);
        }
        List<String> ordered = new ArrayList<>(base);
        Comparator<String> comparator = Comparator.comparingInt(id -> originalIndex.get(id));
        if (adaptiveRoutingEnabled) {
            comparator = Comparator
                    .comparingLong((String id) -> latencyScore(id, originalIndex.get(id)))
                    .thenComparing(comparator);
        }
        if (costAwareRoutingEnabled) {
            comparator = Comparator
                    .comparingInt(costTierRegistry::tier)
                    .thenComparing(comparator);
        }
        ordered.sort(comparator);
        if (!ordered.equals(base)) {
            log.debug("Routing order: {} (chain: {})", ordered, base);
        }
        return ordered;
    }

    private long latencyScore(String providerId, int chainIndex) {
        AiProviderLatencyTracker.Snapshot snap = latencyTracker.snapshot(providerId);
        if (snap.sampleCount() == 0) {
            return 1_000_000L + chainIndex;
        }
        return snap.averageLatencyMs();
    }

    private void recordSuccess(String providerId, long startNanos) {
        circuitBreaker.recordSuccess(providerId);
        if (latencyTracker != null) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            latencyTracker.recordLatency(providerId, latencyMs);
        }
        if (quotaTracker != null) {
            quotaTracker.recordUsage(providerId);
        }
    }
}
