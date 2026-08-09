package com.aistudio.infrastructure.ai;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process daily (UTC) request quotas per AI provider.
 */
public class AiProviderQuotaTracker {

  public record Snapshot(int dailyLimit, int usedToday, Integer remaining, boolean exhausted) {
  }

  private final Map<String, Integer> dailyLimits;
  private final ConcurrentHashMap<String, AtomicInteger> usageCounts = new ConcurrentHashMap<>();
  private volatile String usageDay = currentUtcDay();

  public AiProviderQuotaTracker(String rawQuotas) {
    this.dailyLimits = parse(rawQuotas);
  }

  public boolean isQuotaExhausted(String providerId) {
    Snapshot snapshot = snapshot(providerId);
    return snapshot.dailyLimit() > 0 && snapshot.exhausted();
  }

  public void recordUsage(String providerId) {
    if (providerId == null || providerId.isBlank()) {
      return;
    }
    rollDayIfNeeded();
    String id = normalize(providerId);
    if (dailyLimits.getOrDefault(id, 0) <= 0) {
      return;
    }
    usageCounts.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
  }

  public Snapshot snapshot(String providerId) {
    rollDayIfNeeded();
    String id = normalize(providerId);
    int limit = dailyLimits.getOrDefault(id, 0);
    if (limit <= 0) {
      return new Snapshot(0, 0, null, false);
    }
    int used = usageCounts.computeIfAbsent(id, ignored -> new AtomicInteger()).get();
    int remaining = Math.max(0, limit - used);
    return new Snapshot(limit, used, remaining, used >= limit);
  }

  private void rollDayIfNeeded() {
    String today = currentUtcDay();
    if (!today.equals(usageDay)) {
      synchronized (this) {
        if (!today.equals(usageDay)) {
          usageCounts.clear();
          usageDay = today;
        }
      }
    }
  }

  private static Map<String, Integer> parse(String raw) {
    Map<String, Integer> parsed = new HashMap<>();
    if (raw == null || raw.isBlank()) {
      return parsed;
    }
    String normalized = raw.replace(';', ',');
    for (String part : normalized.split(",")) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String[] keyValue = trimmed.contains("=")
          ? trimmed.split("=", 2)
          : trimmed.split(":", 2);
      if (keyValue.length != 2) {
        continue;
      }
      String providerId = normalize(keyValue[0]);
      try {
        int limit = Integer.parseInt(keyValue[1].trim());
        if (limit > 0) {
          parsed.put(providerId, limit);
        }
      } catch (NumberFormatException ignored) {
        // skip invalid entries
      }
    }
    return parsed;
  }

  private static String currentUtcDay() {
    return LocalDate.now(ZoneOffset.UTC).toString();
  }

  private static String normalize(String providerId) {
    return providerId.trim().toLowerCase(Locale.ROOT);
  }
}
