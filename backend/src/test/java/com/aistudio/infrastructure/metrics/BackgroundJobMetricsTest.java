package com.aistudio.infrastructure.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aistudio.domain.job.JobStatus;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BackgroundJobMetricsTest {

    @Test
    void registersQueueDepthGaugesByStatus() {
        BackgroundJobRepository repository = Mockito.mock(BackgroundJobRepository.class);
        Mockito.when(repository.countByStatus(JobStatus.PENDING)).thenReturn(7L);
        Mockito.when(repository.countByStatus(JobStatus.RUNNING)).thenReturn(2L);
        Mockito.when(repository.countByStatus(JobStatus.SUCCEEDED)).thenReturn(100L);
        Mockito.when(repository.countByStatus(JobStatus.FAILED)).thenReturn(1L);
        Mockito.when(repository.countByStatus(JobStatus.CANCELLED)).thenReturn(0L);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new BackgroundJobMetrics(repository, registry);

        assertEquals(
                7.0,
                registry.get("aistudio.jobs.queue.depth").tag("status", "pending").gauge().value());
        assertEquals(
                2.0,
                registry.get("aistudio.jobs.queue.depth").tag("status", "running").gauge().value());
    }
}
