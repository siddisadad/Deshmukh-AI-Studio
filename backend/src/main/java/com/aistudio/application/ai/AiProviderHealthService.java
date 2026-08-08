package com.aistudio.application.ai;

import com.aistudio.infrastructure.ai.AiProviderCircuitBreaker;
import com.aistudio.infrastructure.ai.AiProviderLatencyTracker;
import com.aistudio.infrastructure.ai.AiProviderRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AiProviderHealthService {

    private final AiProviderRegistry registry;
    private final AiProviderCircuitBreaker circuitBreaker;
    private final AiProviderLatencyTracker latencyTracker;

    public AiProviderHealthService(
            AiProviderRegistry registry,
            AiProviderCircuitBreaker circuitBreaker,
            AiProviderLatencyTracker latencyTracker
    ) {
        this.registry = registry;
        this.circuitBreaker = circuitBreaker;
        this.latencyTracker = latencyTracker;
    }

    public List<ProviderHealth> check(boolean runProbe) {
        List<ProviderHealth> results = new ArrayList<>();
        for (String providerId : registry.configuredProviderIds()) {
            AiProviderPort provider = registry.get(providerId);
            AiProviderCircuitBreaker.Snapshot circuit = circuitBreaker.snapshot(providerId);
            AiProviderLatencyTracker.Snapshot latency = latencyTracker.snapshot(providerId);
            Boolean probeUp = null;
            Instant probedAt = null;
            if (runProbe && provider != null) {
                probeUp = provider.probeHealth();
                probedAt = Instant.now();
            }
            results.add(new ProviderHealth(
                    providerId,
                    true,
                    circuit.state().name().toLowerCase(Locale.ROOT),
                    circuit.failureCount(),
                    circuit.openUntil(),
                    latency.averageLatencyMs() >= 0 ? latency.averageLatencyMs() : null,
                    latency.sampleCount(),
                    probeUp,
                    probedAt
            ));
        }
        return results;
    }

    public record ProviderHealth(
            String id,
            boolean configured,
            String circuitState,
            int failureCount,
            Instant circuitOpenUntil,
            Long averageLatencyMs,
            int latencySampleCount,
            Boolean probeStatus,
            Instant probedAt
    ) {
    }
}
