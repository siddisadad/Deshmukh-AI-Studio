package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aistudio.application.ai.AiProviderPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockAiProviderTest {

    private final MockAiProvider provider = new MockAiProvider();

    @Test
    void summarizeRequirementsReturnsStructuredSummary() {
        var result = provider.generate(new AiProviderPort.AiGenerationRequest(
                "You are a business analyst",
                List.of(new AiProviderPort.AiMessage(
                        "user",
                        "Summarize open requirements for this project."
                )),
                0.3,
                500,
                Map.of()
        ));

        assertThat(result.text()).contains("Open requirements summary");
        assertThat(result.text()).contains("Suggested next steps");
    }
}
