package com.aistudio.infrastructure.metrics;

import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BillingReconciliationMetrics {

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final MeterRegistry meterRegistry;
    private MultiGauge reconciliationDeltaCents;

    public BillingReconciliationMetrics(
            OrganizationSubscriptionRepository subscriptionRepository,
            MeterRegistry meterRegistry
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerGauges() {
        reconciliationDeltaCents = MultiGauge.builder("aistudio.billing.reconciliation.delta.cents")
                .description("Stripe vs internal MTD revenue delta in cents per organization")
                .register(meterRegistry);
        refresh();
    }

    @Scheduled(fixedDelayString = "${aistudio.billing.reconciliation-metrics-refresh-ms:300000}")
    void scheduledRefresh() {
        refresh();
    }

    void refresh() {
        List<OrganizationSubscriptionEntity> subscriptions = subscriptionRepository.findAll();
        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        for (OrganizationSubscriptionEntity sub : subscriptions) {
            if (sub.getReconciliationDeltaCents() == null) {
                continue;
            }
            Tags tags = Tags.of("organization_id", sub.getOrganizationId().toString());
            rows.add(MultiGauge.Row.of(tags, sub.getReconciliationDeltaCents()));
        }
        reconciliationDeltaCents.register(rows, true);
    }
}
