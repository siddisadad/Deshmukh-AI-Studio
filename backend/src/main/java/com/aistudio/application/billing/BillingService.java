package com.aistudio.application.billing;

import com.aistudio.api.billing.dto.BillingOverviewResponse;
import com.aistudio.api.billing.dto.CheckoutResponse;
import com.aistudio.api.billing.dto.InvoiceResponse;
import com.aistudio.api.billing.dto.PlanResponse;
import com.aistudio.api.billing.dto.UsageDayResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.billing.PlanCode;
import com.aistudio.domain.billing.SubscriptionStatus;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import com.aistudio.infrastructure.config.BillingProperties;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private final PlanRepository planRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final AiUsageJdbcRepository usageRepository;
    private final ProjectAuthorizationService authorizationService;
    private final BillingPort billingPort;
    private final BillingProperties billingProperties;
    private final ObjectMapper objectMapper;

    public BillingService(
            PlanRepository planRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            AiUsageJdbcRepository usageRepository,
            ProjectAuthorizationService authorizationService,
            BillingPort billingPort,
            BillingProperties billingProperties,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.usageRepository = usageRepository;
        this.authorizationService = authorizationService;
        this.billingPort = billingPort;
        this.billingProperties = billingProperties;
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
        long memberCount = membershipRepository.countByOrganizationId(organizationId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int usedToday = usageRepository.getCount(organizationId, today);
        int overageToday = usageRepository.getOverageCount(organizationId, today);
        LocalDate periodStart = today.withDayOfMonth(1);
        int periodOverage = usageRepository.sumOverageBetween(organizationId, periodStart, today);
        int seatEstimate = estimateSeatCentsMonthly(plan, memberCount);
        int overageEstimate = periodOverage * plan.getPriceCentsPerAiActionOverage();
        return new BillingOverviewResponse(
                toPlan(plan),
                sub.getStatus().name(),
                billingPort.providerId(),
                sub.getExternalCustomerId(),
                sub.getExternalSubscriptionId(),
                sub.getCurrentPeriodEnd(),
                projectCount,
                plan.getMaxProjects(),
                memberCount,
                plan.getMaxSeats(),
                usedToday,
                overageToday,
                plan.getMaxAiActionsPerDay(),
                periodOverage,
                seatEstimate,
                overageEstimate
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
        long memberCount = membershipRepository.countByOrganizationId(organizationId);
        if (projectCount > plan.getMaxProjects()) {
            throw new DomainException(
                    "PLAN_LIMIT",
                    "Active projects (" + projectCount + ") exceed " + plan.getName() + " limit (" + plan.getMaxProjects() + ")"
            );
        }
        if (memberCount > plan.getMaxSeats()) {
            throw new DomainException(
                    "PLAN_LIMIT",
                    "Members (" + memberCount + ") exceed " + plan.getName() + " seat limit (" + plan.getMaxSeats() + ")"
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
    public List<UsageDayResponse> usageHistory(UUID organizationId, UUID userId, int days) {
        authorizationService.requireOrgMember(organizationId, userId);
        int cappedDays = Math.min(Math.max(days, 1), 90);
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusDays(cappedDays - 1);
        var counts = usageRepository.getCountsBetween(organizationId, start, end);
        var overageCounts = usageRepository.getOverageCountsBetween(organizationId, start, end);
        List<UsageDayResponse> history = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            history.add(new UsageDayResponse(
                    date,
                    counts.getOrDefault(date, 0),
                    overageCounts.getOrDefault(date, 0)
            ));
        }
        return history;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID organizationId, UUID userId, int limit) {
        authorizationService.requireOrgOwner(organizationId, userId);
        return billingPort.listInvoices(organizationId, limit).stream()
                .map(invoice -> new InvoiceResponse(
                        invoice.id(),
                        invoice.number(),
                        invoice.status(),
                        invoice.amountDueCents(),
                        invoice.currency(),
                        invoice.createdAtEpochSeconds() == null
                                ? null
                                : Instant.ofEpochSecond(invoice.createdAtEpochSeconds()),
                        invoice.hostedInvoiceUrl(),
                        invoice.invoicePdfUrl()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String customerPortal(UUID organizationId, UUID userId, String returnUrl) {
        authorizationService.requireOrgOwner(organizationId, userId);
        return billingPort.createCustomerPortalUrl(organizationId, returnUrl);
    }

    @Transactional(readOnly = true)
    public void requireCanAddMember(UUID organizationId) {
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        PlanEntity plan = requirePlan(sub.getPlanCode());
        long memberCount = membershipRepository.countByOrganizationId(organizationId);
        if (memberCount >= plan.getMaxSeats()) {
            throw new DomainException(
                    "PLAN_LIMIT",
                    "Plan " + plan.getName() + " allows " + plan.getMaxSeats()
                            + " members. Upgrade to invite more seats."
            );
        }
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
        if (used < plan.getMaxAiActionsPerDay()) {
            usageRepository.incrementIncluded(organizationId, today);
            return;
        }
        if (plan.getPriceCentsPerAiActionOverage() > 0) {
            usageRepository.incrementOverage(organizationId, today);
            return;
        }
        throw new DomainException(
                "PLAN_LIMIT",
                "Daily AI action limit reached for plan " + plan.getName()
                        + " (" + plan.getMaxAiActionsPerDay() + "/day). Upgrade or try tomorrow."
        );
    }

    @Transactional
    public void requireAndConsumeAiActionForProject(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        requireAndConsumeAiAction(project.getOrganizationId());
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        String secret = billingProperties.stripe().webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new DomainException("CONFIG_ERROR", "Stripe webhook secret is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, secret);
        } catch (SignatureVerificationException ex) {
            throw new DomainException("AUTH_ERROR", "Invalid Stripe webhook signature");
        }
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseGet(() -> {
                    try {
                        return event.getDataObjectDeserializer().deserializeUnsafe();
                    } catch (Exception ex) {
                        return null;
                    }
                });
        if (stripeObject == null) {
            return;
        }
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                if (stripeObject instanceof Session session) {
                    handleCheckoutSessionCompleted(session);
                }
            }
            case "customer.subscription.updated" -> {
                if (stripeObject instanceof Subscription subscription) {
                    handleSubscriptionUpdated(subscription);
                }
            }
            case "customer.subscription.deleted" -> {
                if (stripeObject instanceof Subscription subscription) {
                    handleSubscriptionDeleted(subscription);
                }
            }
            default -> {
                // ignore unhandled events
            }
        }
    }

    @Transactional
    public void applyExternalSubscription(
            UUID organizationId,
            PlanCode planCode,
            String customerId,
            String subscriptionId,
            SubscriptionStatus status,
            Instant periodEnd
    ) {
        OrganizationSubscriptionEntity sub = requireSubscription(organizationId);
        if (customerId != null && !customerId.isBlank()) {
            sub.setExternalCustomerId(customerId);
        }
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            sub.setExternalSubscriptionId(subscriptionId);
        }
        sub.setPlanCode(planCode);
        sub.setStatus(status);
        sub.setCurrentPeriodEnd(periodEnd);
        subscriptionRepository.save(sub);
    }

    private void handleCheckoutSessionCompleted(Session session) {
        UUID organizationId = parseOrganizationId(session);
        if (organizationId == null) {
            return;
        }
        PlanCode planCode = parsePlanCode(session.getMetadata() == null ? null : session.getMetadata().get("planCode"));
        if (planCode == null) {
            return;
        }
        applyExternalSubscription(
                organizationId,
                planCode,
                session.getCustomer(),
                session.getSubscription(),
                SubscriptionStatus.ACTIVE,
                null
        );
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        OrganizationSubscriptionEntity sub = findSubscription(subscription);
        if (sub == null) {
            return;
        }
        PlanCode planCode = resolvePlanFromStripePrice(extractPriceId(subscription));
        if (planCode != null) {
            sub.setPlanCode(planCode);
        }
        sub.setExternalCustomerId(subscription.getCustomer());
        sub.setExternalSubscriptionId(subscription.getId());
        sub.setStatus(mapStripeStatus(subscription.getStatus()));
        if (subscription.getCurrentPeriodEnd() != null) {
            sub.setCurrentPeriodEnd(Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()));
        }
        subscriptionRepository.save(sub);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        OrganizationSubscriptionEntity sub = findSubscription(subscription);
        if (sub == null) {
            return;
        }
        sub.setPlanCode(PlanCode.FREE);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setExternalSubscriptionId(null);
        sub.setCurrentPeriodEnd(null);
        subscriptionRepository.save(sub);
    }

    private OrganizationSubscriptionEntity findSubscription(Subscription subscription) {
        if (subscription.getId() != null) {
            Optional<OrganizationSubscriptionEntity> bySub = subscriptionRepository
                    .findByExternalSubscriptionId(subscription.getId());
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }
        if (subscription.getCustomer() != null) {
            return subscriptionRepository.findByExternalCustomerId(subscription.getCustomer()).orElse(null);
        }
        return null;
    }

    private UUID parseOrganizationId(Session session) {
        if (session.getClientReferenceId() != null && !session.getClientReferenceId().isBlank()) {
            try {
                return UUID.fromString(session.getClientReferenceId());
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (session.getMetadata() != null && session.getMetadata().get("organizationId") != null) {
            try {
                return UUID.fromString(session.getMetadata().get("organizationId"));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private PlanCode parsePlanCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PlanCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private PlanCode resolvePlanFromStripePrice(String priceId) {
        if (priceId == null || priceId.isBlank()) {
            return null;
        }
        return planRepository.findByStripePriceId(priceId)
                .map(PlanEntity::getCode)
                .orElseGet(() -> {
                    BillingProperties.Stripe stripe = billingProperties.stripe();
                    if (priceId.equals(stripe.proPriceId())) {
                        return PlanCode.PRO;
                    }
                    if (priceId.equals(stripe.teamPriceId())) {
                        return PlanCode.TEAM;
                    }
                    return null;
                });
    }

    private String extractPriceId(Subscription subscription) {
        if (subscription.getItems() == null || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        return subscription.getItems().getData().getFirst().getPrice().getId();
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return SubscriptionStatus.ACTIVE;
        }
        switch (stripeStatus) {
            case "trialing":
                return SubscriptionStatus.TRIALING;
            case "past_due":
                return SubscriptionStatus.PAST_DUE;
            case "canceled", "unpaid":
                return SubscriptionStatus.CANCELED;
            default:
                return SubscriptionStatus.ACTIVE;
        }
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
                plan.getMaxSeats(),
                plan.getPriceCentsPerSeatMonthly(),
                plan.getPriceCentsPerAiActionOverage(),
                features
        );
    }

    private static int estimateSeatCentsMonthly(PlanEntity plan, long memberCount) {
        if (plan.getPriceCentsPerSeatMonthly() <= 0 || memberCount <= 1) {
            return plan.getPriceCentsMonthly();
        }
        long extraSeats = memberCount - 1;
        return plan.getPriceCentsMonthly() + (int) (extraSeats * plan.getPriceCentsPerSeatMonthly());
    }
}
