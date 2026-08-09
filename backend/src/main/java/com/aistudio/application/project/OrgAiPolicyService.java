package com.aistudio.application.project;

import com.aistudio.api.organization.dto.OrgAiPolicyResponse;
import com.aistudio.api.organization.dto.UpdateOrgAiPolicyRequest;
import com.aistudio.application.billing.BillingService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.ai.AiProviderCrossRegionRegistry;
import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgAiPolicyService {

    private final ProjectAuthorizationService authorizationService;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final AiUsageJdbcRepository usageRepository;
    private final BillingService billingService;
    private final AiProviderCrossRegionRegistry crossRegionRegistry;

    public OrgAiPolicyService(
            ProjectAuthorizationService authorizationService,
            OrganizationSubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            AiUsageJdbcRepository usageRepository,
            BillingService billingService,
            AiProviderCrossRegionRegistry crossRegionRegistry
    ) {
        this.authorizationService = authorizationService;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.usageRepository = usageRepository;
        this.billingService = billingService;
        this.crossRegionRegistry = crossRegionRegistry;
    }

    @Transactional(readOnly = true)
    public OrgAiPolicyResponse getPolicy(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional
    public OrgAiPolicyResponse updatePolicy(UUID organizationId, UUID userId, UpdateOrgAiPolicyRequest request) {
        authorizationService.requireOrgOwner(organizationId, userId);
        if (request.providerChain() != null) {
            validateProviderChain(request.providerChain());
        }
        if (request.modelMap() != null) {
            validateModelMap(request.modelMap());
        }
        if (request.deployRegion() != null) {
            validateDeployRegion(request.deployRegion());
        }
        OrganizationSubscriptionEntity updated = billingService.updateAiPolicy(
                organizationId,
                request.providerChain(),
                request.dailyTokenBudget(),
                request.modelMap(),
                request.deployRegion());
        return toResponse(updated);
    }

    private OrgAiPolicyResponse toResponse(OrganizationSubscriptionEntity sub) {
        PlanEntity plan = planRepository.findById(sub.getPlanCode())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Plan not found"));
        long effectiveBudget = effectiveBudget(sub, plan);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long used = usageRepository.getTokenCount(sub.getOrganizationId(), today);
        Long remaining = effectiveBudget > 0 ? Math.max(0L, effectiveBudget - used) : null;
        String deployRegion = sub.getAiDeployRegion();
        String effectiveDeployRegion = crossRegionRegistry.effectiveRegion(deployRegion);
        return new OrgAiPolicyResponse(
                sub.getAiProviderChain(),
                sub.getDailyTokenBudget(),
                effectiveBudget,
                used,
                remaining,
                sub.getAiModelMap(),
                deployRegion,
                effectiveDeployRegion
        );
    }

    private long effectiveBudget(OrganizationSubscriptionEntity sub, PlanEntity plan) {
        Long override = sub.getDailyTokenBudget();
        if (override != null && override > 0) {
            return override;
        }
        return plan.getMaxAiTokensPerDay();
    }

    private OrganizationSubscriptionEntity requireSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Subscription not found"));
    }

    private static void validateProviderChain(String chain) {
        if (chain.isBlank()) {
            return;
        }
        for (String part : chain.split(",")) {
            String id = part.trim().toLowerCase();
            if (!id.matches("[a-z0-9._-]+")) {
                throw new DomainException(
                        "VALIDATION_ERROR",
                        "providerChain entries must be lowercase provider ids separated by commas");
            }
        }
    }

    private static void validateModelMap(String map) {
        if (map.isBlank()) {
            return;
        }
        for (String part : map.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.matches("[A-Z_]+=[a-z0-9._-]+:[^,=]+")) {
                throw new DomainException(
                        "VALIDATION_ERROR",
                        "modelMap entries must be ASSISTANT_ROLE=provider:model");
            }
        }
    }

    private static void validateDeployRegion(String region) {
        if (region.isBlank()) {
            return;
        }
        String normalized = region.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9-]+")) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "deployRegion must be lowercase letters, numbers, and hyphens (e.g. us-east, eu-west)");
        }
    }
}
