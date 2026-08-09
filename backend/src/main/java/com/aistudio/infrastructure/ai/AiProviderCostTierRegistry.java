package com.aistudio.infrastructure.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Relative cost tiers per provider for cost-aware routing (lower = cheaper).
 */
public class AiProviderCostTierRegistry {

  public static final int DEFAULT_TIER = 10;

  private static final Map<String, Integer> DEFAULT_TIERS = Map.of(
      "mock", 1,
      "openai", 5,
      "anthropic", 8
  );

  private final Map<String, Integer> tiers;

  public AiProviderCostTierRegistry(String rawTiers) {
    this.tiers = parse(rawTiers);
  }

  public int tier(String providerId) {
    if (providerId == null || providerId.isBlank()) {
      return DEFAULT_TIER;
    }
    String id = normalize(providerId);
    Integer configured = tiers.get(id);
    if (configured != null) {
      return configured;
    }
    Integer defaultTier = DEFAULT_TIERS.get(id);
    if (defaultTier != null) {
      return defaultTier;
    }
    int hyphen = id.indexOf('-');
    if (hyphen > 0) {
      Integer baseTier = DEFAULT_TIERS.get(id.substring(0, hyphen));
      if (baseTier != null) {
        return baseTier;
      }
    }
    return DEFAULT_TIER;
  }

  public List<String> orderByCost(List<String> chain) {
    if (chain.isEmpty()) {
      return List.of();
    }
    Map<String, Integer> originalIndex = new HashMap<>();
    for (int i = 0; i < chain.size(); i++) {
      originalIndex.put(chain.get(i), i);
    }
    List<String> ordered = new ArrayList<>(chain);
    ordered.sort(Comparator
        .comparingInt(this::tier)
        .thenComparingInt(id -> originalIndex.get(id)));
    return ordered;
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
        int tier = Integer.parseInt(keyValue[1].trim());
        if (tier >= 0) {
          parsed.put(providerId, tier);
        }
      } catch (NumberFormatException ignored) {
        // skip invalid entries
      }
    }
    return parsed;
  }

  private static String normalize(String providerId) {
    return providerId.trim().toLowerCase(Locale.ROOT);
  }
}
