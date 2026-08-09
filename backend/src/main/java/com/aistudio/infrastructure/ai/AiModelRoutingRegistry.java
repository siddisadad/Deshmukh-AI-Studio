package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiModelRoute;
import com.aistudio.domain.ai.AssistantRole;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parses assistant-role → provider:model mappings for model-specific routing.
 */
public class AiModelRoutingRegistry {

    private final Map<String, AiModelRoute> routes;

    public AiModelRoutingRegistry(String rawMap) {
        this.routes = parse(rawMap);
    }

    public Optional<AiModelRoute> routeFor(AssistantRole role) {
        if (role == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(routes.get(role.name()));
    }

    public Map<String, AiModelRoute> allRoutes() {
        return Map.copyOf(routes);
    }

    private static Map<String, AiModelRoute> parse(String raw) {
        Map<String, AiModelRoute> parsed = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return parsed;
        }
        String normalized = raw.replace(';', ',');
        for (String part : normalized.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] roleAndRoute = trimmed.split("=", 2);
            if (roleAndRoute.length != 2) {
                continue;
            }
            String role = roleAndRoute[0].trim().toUpperCase(Locale.ROOT);
            String routeValue = roleAndRoute[1].trim();
            String[] providerModel = routeValue.split(":", 2);
            if (providerModel.length != 2) {
                continue;
            }
            String providerId = providerModel[0].trim().toLowerCase(Locale.ROOT);
            String model = providerModel[1].trim();
            if (!providerId.isBlank() && !model.isBlank()) {
                parsed.put(role, new AiModelRoute(providerId, model));
            }
        }
        return parsed;
    }
}
