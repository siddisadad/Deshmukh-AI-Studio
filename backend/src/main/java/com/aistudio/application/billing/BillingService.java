package com.aistudio.application.billing;

import com.aistudio.api.billing.dto.BillingOverviewResponse;
import com.aistudio.api.billing.dto.CheckoutResponse;
import com.aistudio.api.billing.dto.PlanResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.billing.PlanCode;
import com.aistudio.domain.billing.SubscriptionStatus;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final PlanRepository planRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final ProjectRepository projectRepository;
    private final AiUsageJdbcRepository usageRepository;
    private final ProjectAuthorizationService authorizationService;
    private final BillingPort billingPort;
    private final ObjectMapper objectMapper;

    public BillingService(
            PlanRepository planRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            ProjectRepository projectRepository,
            AiUsageJdbcRepository usageRepository,
            ProjectAuthorizationService authorizationService,
            BillingPort billingPort,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.projectRepository = projectRepository;
        this.usageRepository = usageRepository;
        this.authorizationService = authorizationService;
        this.billingPort = billingPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ensureFreeSubscription(UUID organizationId) {
        subscriptionRepository.findByOrganizationId(organizationId).orElseGet(() -> {
            OrganizationSubscriptionEntity sub = new OrganizationSubscriptionEntity();
            sub.setOrganizationId(organizationId);
            sub.setPlanCode(PlanCode.FREE);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            return subscriptionRepository.save(sub);
        });
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listPlans() {
        return planRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getPriceCentsMonthly(), b.getPriceCentsMonthly()))
                .map(this::toPlan)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillingOverviewResponse overview(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        PlanEntity plan = requirePlan(sub.getPlanCode());
        long projectCount = projectRepository.countByOrganizationIdAndStatus(organizationId, ProjectStatus.ACTIVE);
        int usedToday = usageRepository.getCount(organizationId, LocalDate.now(ZoneOffset.UTC));
        return new BillingOverviewResponse(
                toPlan(plan),
                sub.getStatus().name(),
                billingPort.providerId(),
                sub.getExternalCustomerId(),
                sub.getExternalSubscriptionId(),
                sub.getCurrentPeriodEnd(),
                projectCount,
                plan.getMaxProjects(),
                usedToday,
                plan.getMaxAiActionsPerDay()
        );
    }

    @Transactional
    public CheckoutResponse startCheckout(UUID organizationId, UUID userId, PlanCode planCode, String successUrl, String cancelUrl) {
        authorizationService.requireOrgOwner(organizationId, userId);
        if (planCode == PlanCode.FREE) {
            throw new DomainException("VALIDATION_ERROR", "Use change-plan to move to Free");
        }
        requirePlan(planCode);
        BillingPort.CheckoutSession session = billingPort.createCheckoutSession(
                organizationId, planCode, successUrl, cancelUrl);
        return new CheckoutResponse(session.sessionId(), session.checkoutUrl(), billingPort.providerId());
    }

    @Transactional
    public BillingOverviewResponse changePlan(UUID organizationId, UUID userId, PlanCode planCode) {
        authorizationService.requireOrgOwner(organizationId, userId);
        PlanEntity plan = requirePlan(planCode);
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        long projectCount = projectRepository.countByOrganizationIdAndStatus(organizationId, ProjectStatus.ACTIVE);
        if (projectCount > plan.getMaxProjects()) {
            throw new DomainException(
                    "PLAN_LIMIT",
                    "Active projects (" + projectCount + ") exceed " + plan.getName() + " limit (" + plan.getMaxProjects() + ")"
            );
        }
        sub.setPlanCode(planCode);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setExternalSubscriptionId("mock_sub_" + planCode.name().toLowerCase());
        if (sub.getExternalCustomerId() == null) {
            sub.setExternalCustomerId("mock_cus_" + organizationId.toString().replace("-", "").substring(0, 10));
        }
        sub.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        subscriptionRepository.save(sub);
        return overview(organizationId, userId);
    }

    @Transactional(readOnly = true)
    public String customerPortal(UUID organizationId, UUID userId, String returnUrl) {
        authorizationService.requireOrgOwner(organizationId, userId);
        return billingPort.createCustomerPortalUrl(organizationId, returnUrl);
    }

    @Transactional(readOnly = true)
    public void requireCanCreateProject(UUID organizationId) {
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        PlanEntity plan = requirePlan(sub.getPlanCode());
        long projectCount = projectRepository.countByOrganizationIdAndStatus(organizationId, ProjectStatus.ACTIVE);
        if (projectCount >= plan.getMaxProjects()) {
            throw new DomainException(
                    "PLAN_LIMIT",
                    "Plan " + plan.getName() + " allows " + plan.getMaxProjects()
                            + " active projects. Upgrade to create more."
            );
        }
    }

    @Transactional
    public void requireAndConsumeAiAction(UUID organizationId) {
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        PlanEntity plan = requirePlan(sub.getPlanCode());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int used = usageRepository.getCount(organizationId, today);
        if (used >= plan.getMaxAiActionsPerDay()) {
            throw new DomainException(
                    "PLAN_LIMIT",
                    "Daily AI action limit reached for plan " + plan.getName()
                            + " (" + plan.getMaxAiActionsPerDay() + "/day). Upgrade or try tomorrow."
            );
        }
        usageRepository.increment(organizationId, today);
    }

    @Transactional
    public void requireAndConsumeAiActionForProject(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        requireAndConsumeAiAction(project.getOrganizationId());
    }

    private OrganizationSubscriptionEntity requireSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    OrganizationSubscriptionEntity created = new OrganizationSubscriptionEntity();
                    created.setOrganizationId(organizationId);
                    created.setPlanCode(PlanCode.FREE);
                    created.setStatus(SubscriptionStatus.ACTIVE);
                    return subscriptionRepository.save(created);
                });
    }

    private PlanEntity requirePlan(PlanCode code) {
        return planRepository.findById(code)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Plan not found"));
    }

    private PlanResponse toPlan(PlanEntity plan) {
        List<String> features;
        try {
            features = Arrays.asList(objectMapper.readValue(
                    plan.getFeatures() == null ? "[]" : plan.getFeatures(), String[].class));
        } catch (Exception ex) {
            features = List.of();
        }
        return new PlanResponse(
                plan.getCode().name(),
                plan.getName(),
                plan.getPriceCentsMonthly(),
                plan.getMaxProjects(),
                plan.getMaxAiActionsPerDay(),
                features
        );
    }
}
