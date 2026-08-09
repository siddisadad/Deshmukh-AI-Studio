package com.aistudio.application.project;

import com.aistudio.api.organization.dto.UpdateOrgAiPolicyRequest;
import com.aistudio.infrastructure.ai.AiProviderCrossRegionRegistry;
import com.aistudio.infrastructure.ai.AiProviderRegistry;
import com.aistudio.infrastructure.config.AiProperties;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Dry-run resolver for org AI routing policy without persisting changes.
 */
@Service
public class OrgAiPolicyRoutingPreview {

    private final AiProperties aiProperties;
    private final AiProviderCrossRegionRegistry crossRegionRegistry;
    private final AiProviderRegistry providerRegistry;

    public OrgAiPolicyRoutingPreview(
            AiProperties aiProperties,
            AiProviderCrossRegionRegistry crossRegionRegistry,
            AiProviderRegistry providerRegistry
    ) {
        this.aiProperties = aiProperties;
        this.crossRegionRegistry = crossRegionRegistry;
        this.providerRegistry = providerRegistry;
    }

    public Preview preview(
            OrganizationSubscriptionEntity current,
            PlanEntity plan,
            long tokensUsedToday,
            UpdateOrgAiPolicyRequest request
    ) {
        Snapshot currentSnapshot = snapshot(current, plan, tokensUsedToday);
        Merged merged = merge(current, request);
        Snapshot simulatedSnapshot = snapshotFromMerged(merged, plan, tokensUsedToday);

        List<String> currentChain = resolveEffectiveChain(currentSnapshot.deployRegion(), currentSnapshot.providerChain());
        List<String> simulatedChain = resolveEffectiveChain(
                simulatedSnapshot.deployRegion(),
                simulatedSnapshot.providerChain());

        List<String> missing = simulatedChain.stream()
                .filter(id -> providerRegistry.get(id) == null)
                .toList();

        return new Preview(currentSnapshot, simulatedSnapshot, currentChain, simulatedChain, missing);
    }

    private Snapshot snapshot(OrganizationSubscriptionEntity sub, PlanEntity plan, long tokensUsedToday) {
        long budget = effectiveBudget(sub.getDailyTokenBudget(), plan);
        Long remaining = budget > 0 ? Math.max(0L, budget - tokensUsedToday) : null;
        String deployRegion = sub.getAiDeployRegion();
        return new Snapshot(
                sub.getAiProviderChain(),
                sub.getDailyTokenBudget(),
                budget,
                remaining,
                sub.getAiModelMap(),
                deployRegion,
                crossRegionRegistry.effectiveRegion(deployRegion)
        );
    }

    private Snapshot snapshotFromMerged(Merged merged, PlanEntity plan, long tokensUsedToday) {
        long budget = effectiveBudget(merged.dailyTokenBudget(), plan);
        Long remaining = budget > 0 ? Math.max(0L, budget - tokensUsedToday) : null;
        return new Snapshot(
                merged.providerChain(),
                merged.dailyTokenBudget(),
                budget,
                remaining,
                merged.modelMap(),
                merged.deployRegion(),
                crossRegionRegistry.effectiveRegion(merged.deployRegion())
        );
    }

    private Merged merge(OrganizationSubscriptionEntity current, UpdateOrgAiPolicyRequest request) {
        String providerChain = request.providerChain() != null
                ? normalizeOptionalString(request.providerChain())
                : current.getAiProviderChain();
        Long dailyTokenBudget = request.dailyTokenBudget() != null
                ? normalizeTokenBudget(request.dailyTokenBudget())
                : current.getDailyTokenBudget();
        String modelMap = request.modelMap() != null
                ? normalizeOptionalString(request.modelMap())
                : current.getAiModelMap();
        String deployRegion = request.deployRegion() != null
                ? normalizeDeployRegion(request.deployRegion())
                : current.getAiDeployRegion();
        return new Merged(providerChain, dailyTokenBudget, modelMap, deployRegion);
    }

    private List<String> resolveEffectiveChain(String deployRegion, String orgProviderChain) {
        List<String> platformChain = platformChain();
        List<String> regionalChain = crossRegionRegistry.resolveChain(platformChain, deployRegion);
        if (orgProviderChain != null && !orgProviderChain.isBlank()) {
            return parseChain(orgProviderChain);
        }
        return regionalChain;
    }

    private List<String> platformChain() {
        String provider = normalize(aiProperties.provider());
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if ("routing".equals(provider)) {
            return parseList(aiProperties.providerChain());
        }
        List<String> chain = new ArrayList<>();
        chain.add(provider);
        chain.addAll(parseList(aiProperties.providerFallbacks()));
        return chain;
    }

    private static long effectiveBudget(Long override, PlanEntity plan) {
        if (override != null && override > 0) {
            return override;
        }
        return plan.getMaxAiTokensPerDay();
    }

    private static List<String> parseChain(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptionalString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Long normalizeTokenBudget(Long value) {
        if (value == null) {
            return null;
        }
        return value <= 0 ? null : value;
    }

    private static String normalizeDeployRegion(String region) {
        if (region == null) {
            return null;
        }
        String trimmed = region.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record Snapshot(
            String providerChain,
            Long dailyTokenBudget,
            long effectiveDailyTokenBudget,
            Long tokenBudgetRemaining,
            String modelMap,
            String deployRegion,
            String effectiveDeployRegion
    ) {
    }

    private record Merged(
            String providerChain,
            Long dailyTokenBudget,
            String modelMap,
            String deployRegion
    ) {
    }

    public record Preview(
            Snapshot current,
            Snapshot simulated,
            List<String> currentEffectiveProviderChain,
            List<String> simulatedEffectiveProviderChain,
            List<String> missingProviders
    ) {
    }
}
