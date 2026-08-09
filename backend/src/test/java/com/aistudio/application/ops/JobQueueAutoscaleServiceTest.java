package com.aistudio.application.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aistudio.domain.job.JobStatus;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobQueueAutoscaleServiceTest {

    @Mock BackgroundJobRepository jobRepository;

    @Test
    void suggestsReplicasFromPendingDepth() {
        when(jobRepository.countByStatus(JobStatus.PENDING)).thenReturn(25L);
        when(jobRepository.countByStatus(JobStatus.RUNNING)).thenReturn(2L);
        when(jobRepository.countByStatus(JobStatus.FAILED)).thenReturn(1L);
        JobQueueAutoscaleService service = new JobQueueAutoscaleService(jobRepository, 10, 6);
        var metrics = service.metrics();
        assertThat(metrics.pending()).isEqualTo(25);
        assertThat(metrics.suggestedReplicas()).isEqualTo(3);
    }
}
