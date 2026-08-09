package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiProviderQuotaTrackerTest {

    @Test
    void tracksDailyUsageAgainstLimit() {
        AiProviderQuotaTracker tracker = new AiProviderQuotaTracker("mock:2");
        assertThat(tracker.snapshot("mock").exhausted()).isFalse();
        tracker.recordUsage("mock");
        tracker.recordUsage("mock");
        AiProviderQuotaTracker.Snapshot snapshot = tracker.snapshot("mock");
        assertThat(snapshot.usedToday()).isEqualTo(2);
        assertThat(snapshot.exhausted()).isTrue();
        assertThat(tracker.isQuotaExhausted("mock")).isTrue();
    }

    @Test
    void unlimitedWhenQuotaNotConfigured() {
        AiProviderQuotaTracker tracker = new AiProviderQuotaTracker(null);
        tracker.recordUsage("openai");
        assertThat(tracker.isQuotaExhausted("openai")).isFalse();
        assertThat(tracker.snapshot("openai").dailyLimit()).isZero();
    }
}
