package com.aistudio.infrastructure.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.aistudio.infrastructure.billing.AiUsageJdbcRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BillingUsageMetricsTest {

    @Test
    void registersCountersAndIncrements() {
        AiUsageJdbcRepository usageRepository = Mockito.mock(AiUsageJdbcRepository.class);
        MembershipRepository membershipRepository = Mockito.mock(MembershipRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        BillingUsageMetrics metrics = new BillingUsageMetrics(usageRepository, membershipRepository, registry);
        metrics.recordIncludedAction();
        metrics.recordOverageAction();
        metrics.recordOverageAction();

        assertThat(registry.get("aistudio.billing.ai.actions").tag("type", "included").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("aistudio.billing.ai.actions").tag("type", "overage").counter().count())
                .isEqualTo(2.0);
    }
}
