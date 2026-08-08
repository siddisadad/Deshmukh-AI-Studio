package com.aistudio.infrastructure.metrics;

import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Prometheus metrics for seat metering and AI usage-based billing.
 */
@Component
public class BillingUsageMetrics {

    private final Counter includedActions;
    private final Counter overageActions;

    public BillingUsageMetrics(
            AiUsageJdbcRepository usageRepository,
            MembershipRepository membershipRepository,
            MeterRegistry meterRegistry
    ) {
        this.includedActions = Counter.builder("aistudio.billing.ai.actions")
                .description("AI actions consumed (included quota)")
                .tag("type", "included")
                .register(meterRegistry);
        this.overageActions = Counter.builder("aistudio.billing.ai.actions")
                .description("AI actions consumed (overage metering)")
                .tag("type", "overage")
                .register(meterRegistry);

        Gauge.builder("aistudio.billing.ai.actions.today", usageRepository, repo ->
                        repo.sumActionsOnDate(LocalDate.now(ZoneOffset.UTC)))
                .description("Sum of included AI actions across all orgs today (UTC)")
                .register(meterRegistry);
        Gauge.builder("aistudio.billing.ai.overage.today", usageRepository, repo ->
                        repo.sumOverageOnDate(LocalDate.now(ZoneOffset.UTC)))
                .description("Sum of overage AI actions across all orgs today (UTC)")
                .register(meterRegistry);
        Gauge.builder("aistudio.billing.ai.overage.period", usageRepository, repo -> {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            return repo.sumOverageBetweenAllOrgs(today.withDayOfMonth(1), today);
        })
                .description("Sum of overage AI actions MTD across all orgs (UTC calendar month)")
                .register(meterRegistry);
        Gauge.builder("aistudio.billing.seats.active", membershipRepository, MembershipRepository::count)
                .description("Total organization memberships (seat count proxy)")
                .register(meterRegistry);
    }

    public void recordIncludedAction() {
        includedActions.increment();
    }

    public void recordOverageAction() {
        overageActions.increment();
    }
}
