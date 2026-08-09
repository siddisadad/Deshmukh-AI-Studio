package com.aistudio.application.ops;

import com.aistudio.api.ops.dto.JobQueueMetricsResponse;
import com.aistudio.domain.job.JobStatus;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueueAutoscaleService {

    private final BackgroundJobRepository jobRepository;
    private final int targetPendingPerReplica;
    private final int maxReplicas;

    public JobQueueAutoscaleService(
            BackgroundJobRepository jobRepository,
            @Value("${aistudio.jobs.autoscale.target-pending-per-replica:10}") int targetPendingPerReplica,
            @Value("${aistudio.jobs.autoscale.max-replicas:6}") int maxReplicas
    ) {
        this.jobRepository = jobRepository;
        this.targetPendingPerReplica = Math.max(1, targetPendingPerReplica);
        this.maxReplicas = Math.max(1, maxReplicas);
    }

    @Transactional(readOnly = true)
    public JobQueueMetricsResponse metrics() {
        long pending = jobRepository.countByStatus(JobStatus.PENDING);
        long running = jobRepository.countByStatus(JobStatus.RUNNING);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        int suggested = suggestReplicas(pending);
        return new JobQueueMetricsResponse(
                pending,
                running,
                failed,
                suggested,
                targetPendingPerReplica,
                maxReplicas
        );
    }

    private int suggestReplicas(long pending) {
        int suggested = (int) ((pending + targetPendingPerReplica - 1) / targetPendingPerReplica);
        if (suggested < 1) {
            suggested = 1;
        }
        return Math.min(suggested, maxReplicas);
    }
}
