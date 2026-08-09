package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AiProviderCrossRegionRegistryTest {

    @Test
    void resolvesRegionalChainForDeployRegion() {
        AiProviderCrossRegionRegistry registry = new AiProviderCrossRegionRegistry(
                true,
                "eu-west",
                "openai-eu=https://eu.api.openai.com",
                "us-east=openai,anthropic;eu-west=openai-eu,anthropic-eu"
        );

        assertThat(registry.resolveChain(List.of("mock", "openai")))
                .containsExactly("openai-eu", "anthropic-eu");
        assertThat(registry.endpointFor("openai-eu")).isEqualTo("https://eu.api.openai.com");
    }

    @Test
    void returnsDefaultChainWhenDisabled() {
        AiProviderCrossRegionRegistry registry = new AiProviderCrossRegionRegistry(
                false,
                "eu-west",
                "",
                "eu-west=openai-eu"
        );

        assertThat(registry.resolveChain(List.of("openai", "mock")))
                .containsExactly("openai", "mock");
    }
}
