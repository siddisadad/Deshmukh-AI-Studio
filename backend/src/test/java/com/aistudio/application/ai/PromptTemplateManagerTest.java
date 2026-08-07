package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptTemplateManagerTest {

    private final PromptTemplateManager manager = new PromptTemplateManager();

    @Test
    void parsesFrontMatterVersionAndBody() {
        var parsed = PromptTemplateManager.parseFrontMatter("""
                ---
                version: 3
                ---
                Hello {{name}}
                """);

        assertThat(parsed.version()).isEqualTo("3");
        assertThat(parsed.body()).isEqualTo("Hello {{name}}");
    }

    @Test
    void systemPromptsAreVersionTwoAndIncludeQualityRules() {
        assertThat(manager.systemPromptVersion("business_analyst")).isEqualTo("2");
        assertThat(manager.systemPrompt("business_analyst")).contains("Assumption:");
        assertThat(manager.systemPrompt("developer")).contains("trade-offs");
        assertThat(manager.systemPrompt("qa_engineer")).contains("Given/When/Then");
    }

    @Test
    void actionPromptSubstitutesVariablesAndRequiresOutputHeadings() {
        String rendered = manager.actionPrompt("ba_improve", Map.of(
                "project_context", "Project: Portal",
                "title", "Login",
                "description", "Users sign in",
                "instructions", "Keep it short"
        ));

        assertThat(manager.actionPromptVersion("ba_improve")).isEqualTo("2");
        assertThat(rendered).contains("Project: Portal");
        assertThat(rendered).contains("Title: Login");
        assertThat(rendered).contains("## Improved requirement");
        assertThat(rendered).contains("## Open questions");
        assertThat(rendered).doesNotContain("{{title}}");
    }
}
