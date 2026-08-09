package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AiProviderCostTierRegistryTest {

    @Test
    void usesDefaultsForKnownProviders() {
        AiProviderCostTierRegistry registry = new AiProviderCostTierRegistry(null);
        assertThat(registry.tier("mock")).isEqualTo(1);
        assertThat(registry.tier("openai")).isEqualTo(5);
        assertThat(registry.tier("anthropic")).isEqualTo(8);
    }

    @Test
    void parsesCustomTiers() {
        AiProviderCostTierRegistry registry = new AiProviderCostTierRegistry("openai:2,anthropic:9");
        assertThat(registry.tier("openai")).isEqualTo(2);
        assertThat(registry.tier("anthropic")).isEqualTo(9);
    }

    @Test
    void ordersByCostTier() {
        AiProviderCostTierRegistry registry = new AiProviderCostTierRegistry(null);
        assertThat(registry.orderByCost(List.of("anthropic", "mock", "openai")))
                .containsExactly("mock", "openai", "anthropic");
    }
}
