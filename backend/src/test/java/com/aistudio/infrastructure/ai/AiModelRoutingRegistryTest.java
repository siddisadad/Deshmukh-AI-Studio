package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aistudio.domain.ai.AssistantRole;
import org.junit.jupiter.api.Test;

class AiModelRoutingRegistryTest {

    @Test
    void parsesAssistantModelMap() {
        AiModelRoutingRegistry registry = new AiModelRoutingRegistry(
                "DEVELOPER=openai:gpt-4o-mini,QA_ENGINEER=anthropic:claude-sonnet-4-20250514");
        assertThat(registry.routeFor(AssistantRole.DEVELOPER))
                .isPresent()
                .get()
                .satisfies(route -> {
                    assertThat(route.providerId()).isEqualTo("openai");
                    assertThat(route.model()).isEqualTo("gpt-4o-mini");
                });
    }
}
