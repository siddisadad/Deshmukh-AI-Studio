package com.aistudio.application.billing;

import com.aistudio.api.billing.dto.StripeReconciliationResponse;
import com.aistudio.application.billing.BillingPort;
import com.aistudio.domain.billing.PlanCode;
import com.aistudio.infrastructure.config.BillingProperties;
import com.aistudio.infrastructure.persistence.entity.BillingReconciliationRunEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.repository.BillingReconciliationRunRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingReconciliationService {

    private final BillingPort billingPort;
    private final BillingProperties billingProperties;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final MembershipRepository membershipRepository;
    private final AiUsageJdbcRepository usageRepository;
    private final BillingReconciliationRunRepository runRepository;

    public BillingReconciliationService(
            BillingPort billingPort,
            BillingProperties billingProperties,
            OrganizationSubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            MembershipRepository membershipRepository,
            AiUsageJdbcRepository usageRepository,
            BillingReconciliationRunRepository runRepository
    ) {
        this.billingPort = billingPort;
        this.billingProperties = billingProperties;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.membershipRepository = membershipRepository;
        this.usageRepository = usageRepository;
        this.runRepository = runRepository;
    }

    @Transactional
    public StripeReconciliationResponse reconcileStripeRevenue() {
        if (!"stripe".equals(billingPort.providerId())) {
            return new StripeReconciliationResponse(
                    0, 0, 0, 0L, 0L, billingProperties.reconciliationToleranceCents(),
                    Instant.now(), List.of("Billing provider is not stripe"));
        }
        if (!billingProperties.reconciliationEnabled()) {
            return new StripeReconciliationResponse(
                    0, 0, 0, 0L, 0L, billingProperties.reconciliationToleranceCents(),
                    Instant.now(), List.of("Billing reconciliation is disabled"));
        }
        long tolerance = billingProperties.reconciliationToleranceCents();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate periodStart = today.withDayOfMonth(1);
        long periodStartEpoch = periodStart.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long periodEndEpoch = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        List<OrganizationSubscriptionEntity> subscriptions = subscriptionRepository
                .findByExternalSubscriptionIdIsNotNullAndPlanCodeIn(List.of(PlanCode.PRO, PlanCode.TEAM));
        int processed = 0;
        int matched = 0;
        int mismatched = 0;
        long totalInternal = 0L;
        long totalStripe = 0L;
        List<String> messages = new ArrayList<>();
        Instant checkedAt = Instant.now();
        for (OrganizationSubscriptionEntity sub : subscriptions) {
            processed++;
            UUID organizationId = sub.getOrganizationId();
            PlanEntity plan = planRepository.findById(sub.getPlanCode()).orElse(null);
            if (plan == null) {
                messages.add(organizationId + ": skip — plan missing");
                continue;
            }
            long memberCount = membershipRepository.countByOrganizationId(organizationId);
            int periodOverage = usageRepository.sumOverageBetween(organizationId, periodStart, today);
            long internalCents = plan.getPriceCentsMonthly()
                    + estimateSeatCentsMonthly(plan, memberCount)
                    + periodOverage * plan.getPriceCentsPerAiActionOverage();
            long stripeCents = billingPort.sumPaidInvoiceCents(organizationId, periodStartEpoch, periodEndEpoch);
            long delta = stripeCents - internalCents;
            totalInternal += internalCents;
            totalStripe += stripeCents;
            sub.setReconciliationDeltaCents(delta);
            sub.setReconciliationCheckedAt(checkedAt);
            subscriptionRepository.save(sub);
            if (Math.abs(delta) <= tolerance) {
                matched++;
                messages.add(organizationId + ": matched internal=" + internalCents + " stripe=" + stripeCents);
            } else {
                mismatched++;
                messages.add(organizationId + ": mismatch delta=" + delta + " internal=" + internalCents
                        + " stripe=" + stripeCents);
            }
        }
        BillingReconciliationRunEntity run = new BillingReconciliationRunEntity();
        run.setProcessedOrgs(processed);
        run.setMatchedOrgs(matched);
        run.setMismatchedOrgs(mismatched);
        run.setTotalInternalCents(totalInternal);
        run.setTotalStripeCents(totalStripe);
        run.setToleranceCents(tolerance);
        runRepository.save(run);
        return new StripeReconciliationResponse(
                processed, matched, mismatched, totalInternal, totalStripe, tolerance, checkedAt, messages);
    }

    private static int estimateSeatCentsMonthly(PlanEntity plan, long memberCount) {
        long extraSeats = Math.max(0L, memberCount - 1L);
        return (int) (extraSeats * plan.getPriceCentsPerSeatMonthly());
    }
}
