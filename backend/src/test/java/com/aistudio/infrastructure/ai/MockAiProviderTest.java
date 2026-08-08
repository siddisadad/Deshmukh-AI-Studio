package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aistudio.application.ai.AiProviderPort;
import java.util.List;
import java.util.Map;
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

    @Test
    void routesExplicitActionBeforePromptKeywordCollision() {
        String noisyPrompt = """
                Summarize open requirements for this project.
                Requirement: users reset passwords.
                """;

        var result = provider.generate(new AiProviderPort.AiGenerationRequest(
                "Business analyst with summarize requirement context",
                List.of(new AiProviderPort.AiMessage("user", noisyPrompt)),
                0.2,
                500,
                Map.of("action", "ba_acceptance_criteria")
        ));

        assertThat(result.text()).contains("Acceptance criteria");
        assertThat(result.text()).doesNotContain("Open requirements summary");
    }

    @Test
    void routesDocsGenerateActionDespiteRequirementKeywords() {
        var result = provider.generate(new AiProviderPort.AiGenerationRequest(
                "Summarize requirements in README",
                List.of(new AiProviderPort.AiMessage("user", "Generate markdown documentation for requirements.")),
                0.2,
                500,
                Map.of("action", "docs_generate")
        ));

        assertThat(result.text()).contains("Project Documentation");
        assertThat(result.text()).doesNotContain("Open requirements summary");
    }

    @Test
    void estimatesTokenUsageForMetering() {
        var result = provider.generate(new AiProviderPort.AiGenerationRequest(
                "Short system",
                List.of(new AiProviderPort.AiMessage("user", "Hello world")),
                0.2,
                500,
                Map.of()
        ));

        assertThat(result.inputTokens()).isPositive();
        assertThat(result.outputTokens()).isPositive();
    }
}
