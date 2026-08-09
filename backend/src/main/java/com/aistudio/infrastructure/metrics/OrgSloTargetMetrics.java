package com.aistudio.infrastructure.metrics;

import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrgSloTargetMetrics {

    private final OrganizationRepository organizationRepository;
    private final MeterRegistry meterRegistry;
    private MultiGauge availabilityTargets;
    private MultiGauge latencyTargets;
    private MultiGauge latencyThresholds;

    public OrgSloTargetMetrics(OrganizationRepository organizationRepository, MeterRegistry meterRegistry) {
        this.organizationRepository = organizationRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerGauges() {
        availabilityTargets = MultiGauge.builder("aistudio.slo.org.availability.target")
                .description("Per-organization availability SLO target (0–1)")
                .register(meterRegistry);
        latencyTargets = MultiGauge.builder("aistudio.slo.org.latency.target")
                .description("Per-organization latency SLO target (0–1)")
                .register(meterRegistry);
        latencyThresholds = MultiGauge.builder("aistudio.slo.org.latency.threshold.seconds")
                .description("Per-organization latency threshold in seconds")
                .register(meterRegistry);
        refresh();
    }

    @Scheduled(fixedDelayString = "${aistudio.slo.target-metrics-refresh-ms:60000}")
    void scheduledRefresh() {
        refresh();
    }

    void refresh() {
        List<OrganizationEntity> organizations = organizationRepository.findAll();
        List<MultiGauge.Row<?>> availabilityRows = new ArrayList<>();
        List<MultiGauge.Row<?>> latencyRows = new ArrayList<>();
        List<MultiGauge.Row<?>> thresholdRows = new ArrayList<>();
        for (OrganizationEntity org : organizations) {
            Tags tags = Tags.of("organization_id", org.getId().toString());
            availabilityRows.add(MultiGauge.Row.of(tags, org.getSloAvailabilityTarget()));
            latencyRows.add(MultiGauge.Row.of(tags, org.getSloLatencyTarget()));
            thresholdRows.add(MultiGauge.Row.of(tags, org.getSloLatencyThresholdSeconds()));
        }
        availabilityTargets.register(availabilityRows, true);
        latencyTargets.register(latencyRows, true);
        latencyThresholds.register(thresholdRows, true);
    }
}
