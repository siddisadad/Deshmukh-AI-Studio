package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AiProviderLatencyTrackerTest {

    @Test
    void ordersByAverageLatency() {
        AiProviderLatencyTracker tracker = new AiProviderLatencyTracker(10);
        tracker.recordLatency("slow", 500);
        tracker.recordLatency("slow", 400);
        tracker.recordLatency("fast", 50);
        tracker.recordLatency("fast", 60);

        assertThat(tracker.orderByLatency(List.of("slow", "fast"))).containsExactly("fast", "slow");
    }

    @Test
    void preservesChainOrderWhenNoSamples() {
        AiProviderLatencyTracker tracker = new AiProviderLatencyTracker(10);
        assertThat(tracker.orderByLatency(List.of("openai", "anthropic", "mock")))
                .containsExactly("openai", "anthropic", "mock");
    }

    @Test
    void snapshotReportsAverageAndCount() {
        AiProviderLatencyTracker tracker = new AiProviderLatencyTracker(5);
        tracker.recordLatency("mock", 100);
        tracker.recordLatency("mock", 200);

        AiProviderLatencyTracker.Snapshot snap = tracker.snapshot("mock");
        assertThat(snap.sampleCount()).isEqualTo(2);
        assertThat(snap.averageLatencyMs()).isEqualTo(150L);
    }
}
