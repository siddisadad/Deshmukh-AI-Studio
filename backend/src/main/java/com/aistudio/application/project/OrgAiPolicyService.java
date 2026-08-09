package com.aistudio.application.project;

import com.aistudio.api.organization.dto.OrgAiPolicyChangeResponse;
import com.aistudio.api.organization.dto.OrgAiPolicyResponse;
import com.aistudio.api.organization.dto.UpdateOrgAiPolicyRequest;
import com.aistudio.application.billing.BillingService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.ai.OrgAiPolicyChangeStatus;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.infrastructure.ai.AiProviderCrossRegionRegistry;
import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrgAiPolicyChangeEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.repository.OrgAiPolicyChangeRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
    private final OrgAiPolicyChangeRepository changeRepository;
    private final ObjectMapper objectMapper;
    private final boolean changeApprovalRequired;

    public OrgAiPolicyService(
            ProjectAuthorizationService authorizationService,
            OrganizationSubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            AiUsageJdbcRepository usageRepository,
            BillingService billingService,
            AiProviderCrossRegionRegistry crossRegionRegistry,
            OrgAiPolicyChangeRepository changeRepository,
            ObjectMapper objectMapper,
            @Value("${aistudio.ai.policy-change-approval-enabled:false}") boolean changeApprovalRequired
    ) {
        this.authorizationService = authorizationService;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.usageRepository = usageRepository;
        this.billingService = billingService;
        this.crossRegionRegistry = crossRegionRegistry;
        this.changeRepository = changeRepository;
        this.objectMapper = objectMapper;
        this.changeApprovalRequired = changeApprovalRequired;
    }

    @Transactional(readOnly = true)
    public OrgAiPolicyResponse getPolicy(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional(readOnly = true)
    public List<OrgAiPolicyChangeResponse> listChanges(UUID organizationId, UUID userId, int limit) {
        authorizationService.requireOrgMember(organizationId, userId);
        int capped = Math.min(Math.max(limit, 1), 100);
        return changeRepository.findByOrganizationIdOrderByCreatedAtDesc(
                organizationId, PageRequest.of(0, capped))
                .stream()
                .map(this::toChangeResponse)
                .toList();
    }

    @Transactional
    public OrgAiPolicyResponse updatePolicy(UUID organizationId, UUID userId, UpdateOrgAiPolicyRequest request) {
        MembershipEntity membership = authorizationService.requireOrgOwner(organizationId, userId);
        validateRequest(request);
        OrganizationSubscriptionEntity before = requireSubscription(organizationId);
        if (changeApprovalRequired && membership.getRole() == OrgRole.ADMIN) {
            createPendingChange(organizationId, userId, before, request);
            return toResponse(before);
        }
        applyPolicyUpdate(organizationId, userId, userId, before, request);
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional
    public OrgAiPolicyResponse approvePendingChange(UUID organizationId, UUID userId) {
        authorizationService.requireOrgOwnerOnly(organizationId, userId);
        OrgAiPolicyChangeEntity pending = requirePendingChange(organizationId);
        applyFromChangeEntity(organizationId, pending);
        pending.setStatus(OrgAiPolicyChangeStatus.APPLIED);
        pending.setReviewedByUserId(userId);
        pending.setReviewedAt(Instant.now());
        changeRepository.save(pending);
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional
    public OrgAiPolicyResponse rejectPendingChange(UUID organizationId, UUID userId) {
        authorizationService.requireOrgOwnerOnly(organizationId, userId);
        OrgAiPolicyChangeEntity pending = requirePendingChange(organizationId);
        pending.setStatus(OrgAiPolicyChangeStatus.REJECTED);
        pending.setReviewedByUserId(userId);
        pending.setReviewedAt(Instant.now());
        changeRepository.save(pending);
        return toResponse(requireSubscription(organizationId));
    }

    private void applyPolicyUpdate(
            UUID organizationId,
            UUID proposedByUserId,
            UUID reviewedByUserId,
            OrganizationSubscriptionEntity before,
            UpdateOrgAiPolicyRequest request
    ) {
        billingService.updateAiPolicy(
                organizationId,
                request.providerChain(),
                request.dailyTokenBudget(),
                request.modelMap(),
                request.deployRegion());
        recordAppliedChange(organizationId, proposedByUserId, reviewedByUserId, before, request);
    }

    private void applyFromChangeEntity(UUID organizationId, OrgAiPolicyChangeEntity change) {
        billingService.updateAiPolicy(
                organizationId,
                change.getProviderChain(),
                change.getDailyTokenBudget(),
                change.getModelMap(),
                change.getDeployRegion());
    }

    private void createPendingChange(
            UUID organizationId,
            UUID userId,
            OrganizationSubscriptionEntity before,
            UpdateOrgAiPolicyRequest request
    ) {
        changeRepository.findByOrganizationIdAndStatus(organizationId, OrgAiPolicyChangeStatus.PENDING)
                .ifPresent(existing -> changeRepository.delete(existing));
        OrgAiPolicyChangeEntity pending = new OrgAiPolicyChangeEntity();
        pending.setOrganizationId(organizationId);
        pending.setStatus(OrgAiPolicyChangeStatus.PENDING);
        pending.setProposedByUserId(userId);
        pending.setProviderChain(normalizeOptionalString(request.providerChain()));
        pending.setDailyTokenBudget(normalizeTokenBudget(request.dailyTokenBudget()));
        pending.setModelMap(normalizeOptionalString(request.modelMap()));
        pending.setDeployRegion(normalizeDeployRegion(request.deployRegion()));
        pending.setPreviousPolicy(snapshotPolicy(before));
        changeRepository.save(pending);
    }

    private void recordAppliedChange(
            UUID organizationId,
            UUID proposedByUserId,
            UUID reviewedByUserId,
            OrganizationSubscriptionEntity before,
            UpdateOrgAiPolicyRequest request
    ) {
        OrgAiPolicyChangeEntity applied = new OrgAiPolicyChangeEntity();
        applied.setOrganizationId(organizationId);
        applied.setStatus(OrgAiPolicyChangeStatus.APPLIED);
        applied.setProposedByUserId(proposedByUserId);
        applied.setReviewedByUserId(reviewedByUserId);
        applied.setProviderChain(normalizeOptionalString(request.providerChain()));
        applied.setDailyTokenBudget(normalizeTokenBudget(request.dailyTokenBudget()));
        applied.setModelMap(normalizeOptionalString(request.modelMap()));
        applied.setDeployRegion(normalizeDeployRegion(request.deployRegion()));
        applied.setPreviousPolicy(snapshotPolicy(before));
        applied.setReviewedAt(Instant.now());
        changeRepository.save(applied);
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
        OrgAiPolicyChangeResponse pending = changeRepository
                .findByOrganizationIdAndStatus(sub.getOrganizationId(), OrgAiPolicyChangeStatus.PENDING)
                .map(this::toChangeResponse)
                .orElse(null);
        return new OrgAiPolicyResponse(
                sub.getAiProviderChain(),
                sub.getDailyTokenBudget(),
                effectiveBudget,
                used,
                remaining,
                sub.getAiModelMap(),
                deployRegion,
                effectiveDeployRegion,
                changeApprovalRequired,
                pending
        );
    }

    private OrgAiPolicyChangeResponse toChangeResponse(OrgAiPolicyChangeEntity change) {
        return new OrgAiPolicyChangeResponse(
                change.getId(),
                change.getStatus().name(),
                change.getProposedByUserId(),
                change.getReviewedByUserId(),
                change.getProviderChain(),
                change.getDailyTokenBudget(),
                change.getModelMap(),
                change.getDeployRegion(),
                change.getPreviousPolicy(),
                change.getCreatedAt(),
                change.getReviewedAt()
        );
    }

    private OrgAiPolicyChangeEntity requirePendingChange(UUID organizationId) {
        return changeRepository.findByOrganizationIdAndStatus(organizationId, OrgAiPolicyChangeStatus.PENDING)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "No pending AI policy change"));
    }

    private String snapshotPolicy(OrganizationSubscriptionEntity sub) {
        ObjectNode node = objectMapper.createObjectNode();
        if (sub.getAiProviderChain() != null) {
            node.put("providerChain", sub.getAiProviderChain());
        }
        if (sub.getDailyTokenBudget() != null) {
            node.put("dailyTokenBudget", sub.getDailyTokenBudget());
        }
        if (sub.getAiModelMap() != null) {
            node.put("modelMap", sub.getAiModelMap());
        }
        if (sub.getAiDeployRegion() != null) {
            node.put("deployRegion", sub.getAiDeployRegion());
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
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

    private void validateRequest(UpdateOrgAiPolicyRequest request) {
        if (request.providerChain() != null) {
            validateProviderChain(request.providerChain());
        }
        if (request.modelMap() != null) {
            validateModelMap(request.modelMap());
        }
        if (request.deployRegion() != null) {
            validateDeployRegion(request.deployRegion());
        }
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
        String trimmed = region.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
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
