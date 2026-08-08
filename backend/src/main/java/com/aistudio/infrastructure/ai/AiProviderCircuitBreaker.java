package com.aistudio.infrastructure.ai;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-provider circuit breaker — opens after consecutive failures, skips provider until cooldown.
 */
public class AiProviderCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public record Snapshot(State state, int failureCount, Instant openUntil) {
    }

    private static final class CircuitState {
        int failureCount = 0;
        Instant openUntil = null;
    }

    private final boolean enabled;
    private final int failureThreshold;
    private final long openDurationMs;
    private final ConcurrentHashMap<String, CircuitState> states = new ConcurrentHashMap<>();

    public AiProviderCircuitBreaker(boolean enabled, int failureThreshold, int openSeconds) {
        this.enabled = enabled;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDurationMs = Math.max(1L, openSeconds) * 1000L;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean shouldSkip(String providerId) {
        if (!enabled) {
            return false;
        }
        return currentState(providerId) == State.OPEN;
    }

    public State currentState(String providerId) {
        if (!enabled) {
            return State.CLOSED;
        }
        String id = normalize(providerId);
        CircuitState state = states.get(id);
        if (state == null) {
            return State.CLOSED;
        }
        if (state.openUntil != null) {
            if (Instant.now().isBefore(state.openUntil)) {
                return State.OPEN;
            }
            return State.HALF_OPEN;
        }
        return state.failureCount > 0 ? State.HALF_OPEN : State.CLOSED;
    }

    public Snapshot snapshot(String providerId) {
        String id = normalize(providerId);
        CircuitState state = states.get(id);
        if (state == null) {
            return new Snapshot(State.CLOSED, 0, null);
        }
        State circuitState = currentState(providerId);
        return new Snapshot(circuitState, state.failureCount, state.openUntil);
    }

    public void recordSuccess(String providerId) {
        if (!enabled) {
            return;
        }
        states.remove(normalize(providerId));
    }

    public void recordFailure(String providerId) {
        if (!enabled) {
            return;
        }
        String id = normalize(providerId);
        CircuitState state = states.computeIfAbsent(id, ignored -> new CircuitState());
        state.failureCount++;
        if (state.failureCount >= failureThreshold) {
            state.openUntil = Instant.now().plusMillis(openDurationMs);
        }
    }

    private static String normalize(String providerId) {
        return providerId == null ? "" : providerId.trim().toLowerCase(Locale.ROOT);
    }
}
