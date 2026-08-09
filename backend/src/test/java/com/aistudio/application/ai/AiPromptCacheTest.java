package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AiPromptCacheTest {

    @Test
    void cachesComputedValueWhenEnabled() {
        AiPromptCache cache = new AiPromptCache(true, 60, 10);
        AtomicInteger calls = new AtomicInteger();
        String first = cache.getOrCompute("key", () -> {
            calls.incrementAndGet();
            return "value";
        });
        String second = cache.getOrCompute("key", () -> {
            calls.incrementAndGet();
            return "other";
        });
        assertThat(first).isEqualTo("value");
        assertThat(second).isEqualTo("value");
        assertThat(calls.get()).isEqualTo(1);
    }
}
