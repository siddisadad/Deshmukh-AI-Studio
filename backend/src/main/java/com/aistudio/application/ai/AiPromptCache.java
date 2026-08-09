package com.aistudio.application.ai;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TTL cache for assembled chat system prompts and project context strings.
 */
@Component
public class AiPromptCache {

    private record Entry(String value, Instant expiresAt) {
    }

    private final boolean enabled;
    private final long ttlSeconds;
    private final int maxEntries;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public AiPromptCache(
            @Value("${aistudio.ai.prompt-cache.enabled:false}") boolean enabled,
            @Value("${aistudio.ai.prompt-cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${aistudio.ai.prompt-cache.max-entries:500}") int maxEntries
    ) {
        this.enabled = enabled;
        this.ttlSeconds = Math.max(1, ttlSeconds);
        this.maxEntries = Math.max(1, maxEntries);
    }

    public String getOrCompute(String key, Supplier<String> supplier) {
        if (!enabled || key == null || key.isBlank()) {
            return supplier.get();
        }
        Instant now = Instant.now();
        Entry existing = cache.get(key);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            return existing.value();
        }
        String value = supplier.get();
        if (cache.size() >= maxEntries) {
            cache.clear();
        }
        cache.put(key, new Entry(value, now.plusSeconds(ttlSeconds)));
        return value;
    }

    public boolean enabled() {
        return enabled;
    }

    public int size() {
        return cache.size();
    }
}
