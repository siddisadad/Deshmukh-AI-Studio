package com.aistudio.infrastructure.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingUsageMetricsForecastTest {

    @Test
    void linearForecastExtrapolatesToMonthEnd() {
        LocalDate midMonth = LocalDate.of(2026, 8, 15);
        assertThat(BillingUsageMetrics.linearForecastPeriod(150, midMonth)).isEqualTo(310L);
    }

    @Test
    void linearForecastReturnsZeroForEmptyMtd() {
        assertThat(BillingUsageMetrics.linearForecastPeriod(0, LocalDate.of(2026, 8, 8))).isZero();
    }
}
