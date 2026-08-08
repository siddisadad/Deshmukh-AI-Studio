package com.aistudio.infrastructure.metrics;

import com.aistudio.domain.job.JobStatus;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Exposes background job queue depth gauges for Prometheus autoscaling signals.
 */
@Component
public class BackgroundJobMetrics {

    public BackgroundJobMetrics(BackgroundJobRepository jobRepository, MeterRegistry meterRegistry) {
        for (JobStatus status : JobStatus.values()) {
            Gauge.builder("aistudio.jobs.queue.depth", jobRepository, repo -> repo.countByStatus(status))
                    .description("Background job count by status")
                    .tag("status", status.name().toLowerCase())
                    .register(meterRegistry);
        }
    }
}
