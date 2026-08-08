package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.domain.common.AiProviderException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RoutingAiProviderTest {

    @Test
    void failsOverToSecondProviderOnGenerate() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("fail", new FailingProvider("fail"));
        registry.register("mock", new MockAiProvider());

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail", "mock"));
        AiProviderPort.AiGenerationResult result = routing.generate(sampleRequest());

        assertThat(result.text()).contains("Mock AI response");
        assertThat(routing.providerId()).isEqualTo("mock");
    }

    @Test
    void failsOverToSecondProviderOnStream() {
        AiProviderRegistry registry = new AiProviderRegistry();
        registry.register("fail", new FailingProvider("fail"));
        registry.register("mock", new MockAiProvider());

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail", "mock"));
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

        RoutingAiProvider routing = new RoutingAiProvider(registry, List.of("fail1", "fail2"));

        assertThatThrownBy(() -> routing.generate(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("fail2");
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
}
