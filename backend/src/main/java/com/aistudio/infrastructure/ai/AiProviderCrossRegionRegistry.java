package com.aistudio.infrastructure.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps deploy regions to provider chains and optional regional endpoint overrides.
 */
public class AiProviderCrossRegionRegistry {

    private final boolean enabled;
    private final String deployRegion;
    private final Map<String, String> endpointMap;
    private final Map<String, List<String>> regionChains;

    public AiProviderCrossRegionRegistry(
            boolean enabled,
            String deployRegion,
            String endpointMapRaw,
            String regionChainsRaw
    ) {
        this.enabled = enabled;
        this.deployRegion = normalizeRegion(deployRegion);
        this.endpointMap = parseEndpointMap(endpointMapRaw);
        this.regionChains = parseRegionChains(regionChainsRaw);
    }

    public boolean enabled() {
        return enabled;
    }

    public String deployRegion() {
        return deployRegion;
    }

    public Map<String, String> endpointMap() {
        return Map.copyOf(endpointMap);
    }

    public List<String> resolveChain(List<String> defaultChain) {
        return resolveChain(defaultChain, null);
    }

    public List<String> resolveChain(List<String> defaultChain, String regionOverride) {
        if (!enabled) {
            return defaultChain;
        }
        String region = normalizeRegion(regionOverride);
        if (region == null || region.isBlank()) {
            region = deployRegion;
        }
        if (region == null || region.isBlank()) {
            return defaultChain;
        }
        List<String> regional = regionChains.get(region);
        if (regional == null || regional.isEmpty()) {
            return defaultChain;
        }
        return regional;
    }

    public String effectiveRegion(String regionOverride) {
        if (!enabled) {
            return null;
        }
        String region = normalizeRegion(regionOverride);
        if (region != null && !region.isBlank()) {
            return region;
        }
        return deployRegion == null || deployRegion.isBlank() ? null : deployRegion;
    }

    public String endpointFor(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        return endpointMap.get(normalizeProvider(providerId));
    }

    private static Map<String, String> parseEndpointMap(String raw) {
        Map<String, String> parsed = new HashMap<>();
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
            String providerId = normalizeProvider(keyValue[0]);
            String baseUrl = keyValue[1].trim();
            if (!providerId.isEmpty() && !baseUrl.isEmpty()) {
                parsed.put(providerId, baseUrl);
            }
        }
        return parsed;
    }

    private static Map<String, List<String>> parseRegionChains(String raw) {
        Map<String, List<String>> parsed = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return parsed;
        }
        for (String segment : raw.split(";")) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] keyValue = trimmed.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            String region = normalizeRegion(keyValue[0]);
            List<String> chain = Arrays.stream(keyValue[1].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
            if (!region.isEmpty() && !chain.isEmpty()) {
                parsed.put(region, new ArrayList<>(chain));
            }
        }
        return parsed;
    }

    private static String normalizeProvider(String providerId) {
        return providerId.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRegion(String region) {
        if (region == null) {
            return "";
        }
        return region.trim().toLowerCase(Locale.ROOT);
    }
}
