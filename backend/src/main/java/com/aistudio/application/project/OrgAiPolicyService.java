package com.aistudio.application.project;

import com.aistudio.api.organization.dto.OrgAiPolicyChangeResponse;
import com.aistudio.api.organization.dto.OrgAiPolicyResponse;
import com.aistudio.api.organization.dto.OrgAiPolicySimulationRecordResponse;
import com.aistudio.api.organization.dto.OrgAiPolicySimulationResponse;
import com.aistudio.api.organization.dto.OrgAiPolicySnapshotDto;
import com.aistudio.api.organization.dto.UpdateOrgAiCanaryRequest;
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
import com.aistudio.infrastructure.persistence.entity.OrgAiPolicySimulationEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.repository.OrgAiPolicyChangeRepository;
import com.aistudio.infrastructure.persistence.repository.OrgAiPolicySimulationRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
    private final OrgAiPolicySimulationRepository simulationRepository;
    private final OrgAiPolicyRoutingPreview routingPreview;
    private final ObjectMapper objectMapper;
    private final boolean changeApprovalRequired;
    private final boolean simulationGateEnabled;
    private final int simulationGateTtlMinutes;

    public OrgAiPolicyService(
            ProjectAuthorizationService authorizationService,
            OrganizationSubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            AiUsageJdbcRepository usageRepository,
            BillingService billingService,
            AiProviderCrossRegionRegistry crossRegionRegistry,
            OrgAiPolicyChangeRepository changeRepository,
            OrgAiPolicySimulationRepository simulationRepository,
            OrgAiPolicyRoutingPreview routingPreview,
            ObjectMapper objectMapper,
            @Value("${aistudio.ai.policy-change-approval-enabled:false}") boolean changeApprovalRequired,
            @Value("${aistudio.ai.policy-simulation-gate-enabled:false}") boolean simulationGateEnabled,
            @Value("${aistudio.ai.policy-simulation-gate-ttl-minutes:30}") int simulationGateTtlMinutes
    ) {
        this.authorizationService = authorizationService;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.usageRepository = usageRepository;
        this.billingService = billingService;
        this.crossRegionRegistry = crossRegionRegistry;
        this.changeRepository = changeRepository;
        this.simulationRepository = simulationRepository;
        this.routingPreview = routingPreview;
        this.objectMapper = objectMapper;
        this.changeApprovalRequired = changeApprovalRequired;
        this.simulationGateEnabled = simulationGateEnabled;
        this.simulationGateTtlMinutes = simulationGateTtlMinutes;
    }

    @Transactional(readOnly = true)
    public OrgAiPolicyResponse getPolicy(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional
    public OrgAiPolicySimulationResponse simulatePolicy(
            UUID organizationId,
            UUID userId,
            UpdateOrgAiPolicyRequest request
    ) {
        MembershipEntity membership = authorizationService.requireOrgOwner(organizationId, userId);
        validateRequest(request);
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        PlanEntity plan = planRepository.findById(sub.getPlanCode())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Plan not found"));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long used = usageRepository.getTokenCount(sub.getOrganizationId(), today);
        OrgAiPolicyRoutingPreview.Preview preview = routingPreview.preview(sub, plan, used, request);
        boolean gatePassed = preview.missingProviders().isEmpty();
        boolean wouldRequireApproval = changeApprovalRequired && membership.getRole() == OrgRole.ADMIN;
        OrgAiPolicySimulationEntity audit = persistSimulation(
                organizationId,
                userId,
                request,
                preview,
                gatePassed);
        return new OrgAiPolicySimulationResponse(
                audit.getId(),
                toSnapshotDto(preview.current()),
                toSnapshotDto(preview.simulated()),
                preview.currentEffectiveProviderChain(),
                preview.simulatedEffectiveProviderChain(),
                preview.missingProviders(),
                gatePassed,
                wouldRequireApproval
        );
    }

    @Transactional(readOnly = true)
    public List<OrgAiPolicySimulationRecordResponse> listSimulations(
            UUID organizationId,
            UUID userId,
            int limit
    ) {
        authorizationService.requireOrgMember(organizationId, userId);
        int capped = Math.min(Math.max(limit, 1), 100);
        return simulationRepository.findByOrganizationIdOrderByCreatedAtDesc(
                organizationId, PageRequest.of(0, capped))
                .stream()
                .map(this::toSimulationRecordResponse)
                .toList();
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
        OrgAiPolicySimulationEntity simulation = requireSimulationGate(organizationId, userId, request);
        OrganizationSubscriptionEntity before = requireSubscription(organizationId);
        if (changeApprovalRequired && membership.getRole() == OrgRole.ADMIN) {
            OrgAiPolicyChangeEntity pending = createPendingChange(organizationId, userId, before, request);
            linkSimulationToChange(simulation, pending.getId());
            return toResponse(before);
        }
        OrgAiPolicyChangeEntity applied = applyPolicyUpdate(organizationId, userId, userId, before, request);
        linkSimulationToChange(simulation, applied.getId());
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

    @Transactional
    public OrgAiPolicyResponse updateCanary(
            UUID organizationId,
            UUID userId,
            UpdateOrgAiCanaryRequest request
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        validateProviderChain(request.providerChain());
        billingService.updateCanary(organizationId, request.providerChain(), request.percent());
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional
    public OrgAiPolicyResponse promoteCanary(UUID organizationId, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        billingService.promoteCanary(organizationId);
        return toResponse(requireSubscription(organizationId));
    }

    @Transactional
    public OrgAiPolicyResponse abortCanary(UUID organizationId, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        billingService.abortCanary(organizationId);
        return toResponse(requireSubscription(organizationId));
    }

    private OrgAiPolicyChangeEntity applyPolicyUpdate(
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
        return recordAppliedChange(organizationId, proposedByUserId, reviewedByUserId, before, request);
    }

    private void applyFromChangeEntity(UUID organizationId, OrgAiPolicyChangeEntity change) {
        billingService.updateAiPolicy(
                organizationId,
                change.getProviderChain(),
                change.getDailyTokenBudget(),
                change.getModelMap(),
                change.getDeployRegion());
    }

    private OrgAiPolicyChangeEntity createPendingChange(
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
        return pending;
    }

    private OrgAiPolicyChangeEntity recordAppliedChange(
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
        return applied;
    }

    private OrgAiPolicySimulationEntity persistSimulation(
            UUID organizationId,
            UUID userId,
            UpdateOrgAiPolicyRequest request,
            OrgAiPolicyRoutingPreview.Preview preview,
            boolean gatePassed
    ) {
        OrgAiPolicySimulationEntity entity = new OrgAiPolicySimulationEntity();
        entity.setOrganizationId(organizationId);
        entity.setSimulatedByUserId(userId);
        entity.setProviderChain(normalizeOptionalString(request.providerChain()));
        entity.setDailyTokenBudget(normalizeTokenBudget(request.dailyTokenBudget()));
        entity.setModelMap(normalizeOptionalString(request.modelMap()));
        entity.setDeployRegion(normalizeDeployRegion(request.deployRegion()));
        entity.setMissingProviders(writeJsonList(preview.missingProviders()));
        entity.setCurrentEffectiveChain(writeJsonList(preview.currentEffectiveProviderChain()));
        entity.setSimulatedEffectiveChain(writeJsonList(preview.simulatedEffectiveProviderChain()));
        entity.setGatePassed(gatePassed);
        simulationRepository.save(entity);
        return entity;
    }

    private OrgAiPolicySimulationEntity requireSimulationGate(
            UUID organizationId,
            UUID userId,
            UpdateOrgAiPolicyRequest request
    ) {
        if (!simulationGateEnabled) {
            return null;
        }
        if (request.simulationId() == null) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "simulationId is required when AI policy simulation gate is enabled");
        }
        OrgAiPolicySimulationEntity simulation = simulationRepository
                .findByIdAndOrganizationId(request.simulationId(), organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Policy simulation not found"));
        if (!simulation.getSimulatedByUserId().equals(userId)) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Policy simulation must be run by the user applying the change");
        }
        if (!simulation.isGatePassed()) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Policy simulation did not pass rollout gates (missing providers)");
        }
        Instant cutoff = Instant.now().minusSeconds(simulationGateTtlMinutes * 60L);
        if (simulation.getCreatedAt().isBefore(cutoff)) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Policy simulation expired; run simulate again before applying");
        }
        if (!matchesProposedFields(simulation, request)) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Policy fields do not match the recorded simulation");
        }
        if (simulation.getAppliedChangeId() != null) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Policy simulation was already used to apply a change");
        }
        return simulation;
    }

    private void linkSimulationToChange(OrgAiPolicySimulationEntity simulation, UUID changeId) {
        if (simulation == null) {
            return;
        }
        simulation.setAppliedChangeId(changeId);
        simulationRepository.save(simulation);
    }

    private boolean matchesProposedFields(OrgAiPolicySimulationEntity simulation, UpdateOrgAiPolicyRequest request) {
        return nullableEquals(simulation.getProviderChain(), normalizeOptionalString(request.providerChain()))
                && nullableEquals(simulation.getDailyTokenBudget(), normalizeTokenBudget(request.dailyTokenBudget()))
                && nullableEquals(simulation.getModelMap(), normalizeOptionalString(request.modelMap()))
                && nullableEquals(simulation.getDeployRegion(), normalizeDeployRegion(request.deployRegion()));
    }

    private static boolean nullableEquals(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
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
                simulationGateEnabled,
                sub.getAiCanaryProviderChain(),
                sub.getAiCanaryPercent(),
                pending
        );
    }

    private static OrgAiPolicySnapshotDto toSnapshotDto(OrgAiPolicyRoutingPreview.Snapshot snapshot) {
        return new OrgAiPolicySnapshotDto(
                snapshot.providerChain(),
                snapshot.dailyTokenBudget(),
                snapshot.effectiveDailyTokenBudget(),
                snapshot.tokenBudgetRemaining(),
                snapshot.modelMap(),
                snapshot.deployRegion(),
                snapshot.effectiveDeployRegion()
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

    private OrgAiPolicySimulationRecordResponse toSimulationRecordResponse(OrgAiPolicySimulationEntity simulation) {
        return new OrgAiPolicySimulationRecordResponse(
                simulation.getId(),
                simulation.getSimulatedByUserId(),
                simulation.getProviderChain(),
                simulation.getDailyTokenBudget(),
                simulation.getModelMap(),
                simulation.getDeployRegion(),
                readJsonList(simulation.getMissingProviders()),
                readJsonList(simulation.getCurrentEffectiveChain()),
                readJsonList(simulation.getSimulatedEffectiveChain()),
                simulation.isGatePassed(),
                simulation.getAppliedChangeId(),
                simulation.getCreatedAt()
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

    private String writeJsonList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> readJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
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
