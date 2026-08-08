package com.aistudio.infrastructure.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rolling per-provider latency samples for adaptive routing order.
 */
public class AiProviderLatencyTracker {

    public record Snapshot(long averageLatencyMs, int sampleCount) {
    }

    private static final class LatencySamples {
        private final long[] buffer;
        private int count = 0;
        private int head = 0;

        LatencySamples(int sampleSize) {
            this.buffer = new long[sampleSize];
        }

        synchronized void add(long latencyMs) {
            buffer[head] = latencyMs;
            head = (head + 1) % buffer.length;
            if (count < buffer.length) {
                count++;
            }
        }

        synchronized Snapshot snapshot() {
            if (count == 0) {
                return new Snapshot(-1L, 0);
            }
            long sum = 0L;
            for (int i = 0; i < count; i++) {
                sum += buffer[i];
            }
            return new Snapshot(sum / count, count);
        }
    }

    private final int sampleSize;
    private final ConcurrentHashMap<String, LatencySamples> samples = new ConcurrentHashMap<>();

    public AiProviderLatencyTracker(int sampleSize) {
        this.sampleSize = Math.max(1, sampleSize);
    }

    public void recordLatency(String providerId, long latencyMs) {
        if (providerId == null || providerId.isBlank() || latencyMs < 0) {
            return;
        }
        String id = normalize(providerId);
        samples.computeIfAbsent(id, ignored -> new LatencySamples(sampleSize)).add(latencyMs);
    }

    public Snapshot snapshot(String providerId) {
        LatencySamples state = samples.get(normalize(providerId));
        if (state == null) {
            return new Snapshot(-1L, 0);
        }
        return state.snapshot();
    }

    /**
     * Order chain by average latency (fastest first). Providers without samples keep chain order via index bias.
     */
    public List<String> orderByLatency(List<String> chain) {
        if (chain.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> originalIndex = new HashMap<>();
        for (int i = 0; i < chain.size(); i++) {
            originalIndex.put(chain.get(i), i);
        }
        List<String> ordered = new ArrayList<>(chain);
        ordered.sort(Comparator
                .comparingLong((String id) -> latencyScore(id, originalIndex.get(id)))
                .thenComparingInt(id -> originalIndex.get(id)));
        return ordered;
    }

    private long latencyScore(String providerId, int chainIndex) {
        Snapshot snap = snapshot(providerId);
        if (snap.sampleCount() == 0) {
            return 1_000_000L + chainIndex;
        }
        return snap.averageLatencyMs();
    }

    private static String normalize(String providerId) {
        return providerId.trim().toLowerCase(Locale.ROOT);
    }
}
