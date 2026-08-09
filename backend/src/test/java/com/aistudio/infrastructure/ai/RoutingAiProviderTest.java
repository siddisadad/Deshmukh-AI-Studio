package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aistudio.application.ai.AiModelRoute;
import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.application.ai.OrgAiRoutingContext;
import com.aistudio.domain.common.AiProviderException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RoutingAiProviderTest {

    private static AiProviderCircuitBreaker disabledBreaker() {
        return new AiProviderCircuitBreaker(false, 3, 60);
    }

    @Test
    void failsOverToSecondProviderOnGenerate() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("fail", new FailingProvider("fail"));
        registry.register("mock", new MockAiProvider());

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail", "mock"), disabledBreaker());
        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());

        assertThat(result.text()).contains("Mock AI response");
        assertThat(routing.providerId()).isEqualTo("mock");
    }

    @Test
    void failsOverToSecondProviderOnStream() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("fail", new FailingProvider("fail"));
        registry.register("mock", new MockAiProvider());

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail", "mock"), disabledBreaker());
        AtomicReference<String> streamed = new AtomicReference<>("");
        AiProviderPort.AiGenerationResult result = routing.stream(sampleRequest(), delta -> {
            streamed.set(streamed.get() + delta);
        });

        assertThat(streamed.get()).isNotBlank();
        assertThat(result.text()).isEqualTo(streamed.get());
        assertThat(routing.providerId()).isEqualTo("mock");
    }

    @Test
    void throwsWhenAllProvidersFail() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("fail1", new FailingProvider("fail1"));
        registry.register("fail2", new FailingProvider("fail2"));

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail1", "fail2"), disabledBreaker());

        assertThatThrownBy(() -> routing.generate(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("fail2");
    }

    @Test
    void skipsProviderWhenCircuitOpen() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("fail", new FailingProvider("fail"));
        registry.register("mock", new MockAiProvider());

        AiProviderCircuitBreaker breaker = new AiProviderCircuitBreaker(true, 1, 60);
        breaker.recordFailure("fail");

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail", "mock"), breaker);
        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());

        assertThat(result.text()).contains("Mock AI response");
        assertThat(routing.providerId()).isEqualTo("mock");
    }

    @Test
    void adaptiveRoutingPrefersFasterProvider() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("slow", new SlowProvider("slow", 40));
        registry.register("fast", new FastProvider("fast"));

        AiProviderLatencyTracker tracker = new AiProviderLatencyTracker(10);
        tracker.recordLatency("slow", 500);
        tracker.recordLatency("fast", 20);

        RoutingAiProvider routing = new RoutingAiProvider(
                registry,
                List.of("slow", "fast"),
                disabledBreaker(),
                tracker,
                true
        );

        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());
        assertThat(result.text()).contains("fast response");
        assertThat(routing.providerId()).isEqualTo("fast");
    }

    @Test
    void costAwareRoutingPrefersCheaperProvider() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("expensive", new FastProvider("expensive"));
        registry.register("cheap", new FastProvider("cheap"));

        AiProviderCostTierRegistry costTiers = new AiProviderCostTierRegistry("expensive:9,cheap:1");

        RoutingAiProvider routing = new RoutingAiProvider(
                registry,
                List.of("expensive", "cheap"),
                disabledBreaker(),
                null,
                false,
                costTiers,
                null,
                true
        );

        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());
        assertThat(result.text()).contains("cheap response");
        assertThat(routing.providerId()).isEqualTo("cheap");
    }

    @Test
    void skipsProviderWhenQuotaExhausted() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("limited", new FastProvider("limited"));
        registry.register("mock", new MockAiProvider());

        AiProviderQuotaTracker quotas = new AiProviderQuotaTracker("limited:1");
        quotas.recordUsage("limited");

        RoutingAiProvider routing = new RoutingAiProvider(
                registry,
                List.of("limited", "mock"),
                disabledBreaker(),
                null,
                false,
                null,
                quotas,
                false
        );

        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());
        assertThat(result.text()).contains("Mock AI response");
        assertThat(routing.providerId()).isEqualTo("mock");
    }

    @Test
    void modelRoutePrefersMappedProvider() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("openai", new FastProvider("openai"));
        registry.register("mock", new MockAiProvider());

        OrgAiRoutingContext.setModelRoute(new AiModelRoute("openai", "gpt-4o-mini"));

        RoutingAiProvider routing = new RoutingAiProvider(
                registry,
                List.of("mock", "openai"),
                disabledBreaker()
        );
        try {
            AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());
            assertThat(routing.providerId()).isEqualTo("openai");
            assertThat(result.model()).contains("openai");
        } finally {
            OrgAiRoutingContext.clear();
        }
    }

    @Test
    void crossRegionRoutingUsesOrgDeployRegionOverride() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("openai", new FastProvider("openai"));
        registry.register("openai-eu", new FastProvider("openai-eu"));

        AiProviderCrossRegionRegistry crossRegion = new AiProviderCrossRegionRegistry(
                true,
                "us-east",
                "openai-eu=https://eu.api.openai.com",
                "us-east=openai;eu-west=openai-eu"
        );

        OrgAiRoutingContext.setDeployRegion("eu-west");

        RoutingAiProvider routing = new RoutingAiProvider(
                registry,
                List.of("openai"),
                disabledBreaker(),
                null,
                false,
                null,
                null,
                false,
                null,
                crossRegion
        );
        try {
            AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());
            assertThat(routing.providerId()).isEqualTo("openai-eu");
            assertThat(result.text()).contains("openai-eu response");
        } finally {
            OrgAiRoutingContext.clear();
        }
    }

    @Test
    void crossRegionRoutingUsesRegionalChain() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("openai", new FastProvider("openai"));
        registry.register("openai-eu", new FastProvider("openai-eu"));

        AiProviderCrossRegionRegistry crossRegion = new AiProviderCrossRegionRegistry(
                true,
                "eu-west",
                "openai-eu=https://eu.api.openai.com",
                "eu-west=openai-eu,openai;us-east=openai"
        );

        RoutingAiProvider routing = new RoutingAiProvider(
                registry,
                List.of("openai"),
                disabledBreaker(),
                null,
                false,
                null,
                null,
                false,
                null,
                crossRegion
        );

        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());
        assertThat(routing.providerId()).isEqualTo("openai-eu");
        assertThat(result.text()).contains("openai-eu response");
    }

    private static AiProviderPort.AiGenerationRequest sampleRequest() {
        return new AiProviderPort.AiGenerationRequest(
                "You are helpful.",
                List.of(new AiProviderPort.AiMessage("user", "Hello")),
                0.2,
                256,
                null
        );
    }

    private static final class FailingProvider implements AiProviderPort {
        private final String id;

        FailingProvider(String id) {
            this.id = id;
        }

        @Override
        public AiGenerationResult generate(AiGenerationRequest request) {
            throw new AiProviderException(id + " failed");
        }

        @Override
        public String providerId() {
            return id;
        }
    }

    private static final class SlowProvider implements AiProviderPort {
        private final String id;
        private final long delayMs;

        SlowProvider(String id, long delayMs) {
            this.id = id;
            this.delayMs = delayMs;
        }

        @Override
        public AiGenerationResult generate(AiGenerationRequest request) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return new AiGenerationResult(id + " response", id + "-model", 1, 1);
        }

        @Override
        public String providerId() {
            return id;
        }
    }

    private static final class FastProvider implements AiProviderPort {
        private final String id;

        FastProvider(String id) {
            this.id = id;
        }

        @Override
        public AiGenerationResult generate(AiGenerationRequest request) {
            return new AiGenerationResult(id + " response", id + "-model", 1, 1);
        }

        @Override
        public String providerId() {
            return id;
        }
    }
}
