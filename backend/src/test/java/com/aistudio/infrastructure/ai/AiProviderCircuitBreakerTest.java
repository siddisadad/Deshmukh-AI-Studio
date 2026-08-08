package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiProviderCircuitBreakerTest {

    @Test
    void opensAfterThresholdFailures() {
        AiProviderCircuitBreaker breaker = new AiProviderCircuitBreaker(true, 3, 60);
        breaker.recordFailure("openai");
        breaker.recordFailure("openai");
        assertThat(breaker.shouldSkip("openai")).isFalse();

        breaker.recordFailure("openai");
        assertThat(breaker.shouldSkip("openai")).isTrue();
        assertThat(breaker.snapshot("openai").state()).isEqualTo(AiProviderCircuitBreaker.State.OPEN);
    }

    @Test
    void successResetsFailures() {
        AiProviderCircuitBreaker breaker = new AiProviderCircuitBreaker(true, 3, 60);
        breaker.recordFailure("openai");
        breaker.recordFailure("openai");
        breaker.recordSuccess("openai");
        assertThat(breaker.snapshot("openai").failureCount()).isZero();
        assertThat(breaker.shouldSkip("openai")).isFalse();
    }

    @Test
    void disabledNeverSkips() {
        AiProviderCircuitBreaker breaker = new AiProviderCircuitBreaker(false, 3, 60);
        breaker.recordFailure("openai");
        breaker.recordFailure("openai");
        breaker.recordFailure("openai");
        assertThat(breaker.shouldSkip("openai")).isFalse();
    }

    @Test
    void opensWithSingleFailureWhenThresholdIsOne() {
        AiProviderCircuitBreaker breaker = new AiProviderCircuitBreaker(true, 1, 60);
        breaker.recordFailure("mock");
        assertThat(breaker.shouldSkip("mock")).isTrue();
        assertThat(breaker.snapshot("mock").state()).isEqualTo(AiProviderCircuitBreaker.State.OPEN);
    }
}
