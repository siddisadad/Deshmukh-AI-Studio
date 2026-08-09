package com.aistudio.application.job;

import com.aistudio.api.job.dto.JobResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.job.JobStatus;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.persistence.entity.BackgroundJobEntity;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackgroundJobService {

    private final BackgroundJobRepository jobRepository;
    private final ProjectAuthorizationService authorizationService;

    public BackgroundJobService(
            BackgroundJobRepository jobRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.jobRepository = jobRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public JobResponse enqueue(
            UUID projectId,
            UUID userId,
            JobType jobType,
            String payloadJson
    ) {
        authorizationService.requireProjectEdit(projectId, userId);
        return enqueueInternal(projectId, userId, jobType, payloadJson);
    }

    @Transactional
    public JobResponse enqueueInternal(UUID projectId, JobType jobType, Object payload) {
        return enqueueInternal(projectId, null, jobType, payload == null ? "{}" : toJson(payload));
    }

    @Transactional
    public JobResponse enqueueInternal(UUID projectId, UUID userId, JobType jobType, String payloadJson) {
        BackgroundJobEntity job = new BackgroundJobEntity();
        job.setProjectId(projectId);
        job.setCreatedBy(userId);
        job.setJobType(jobType);
        job.setStatus(JobStatus.PENDING);
        job.setPayload(payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
        job.setResult("{}");
        jobRepository.save(job);
        return toResponse(job);
    }

    private String toJson(Object payload) {
        if (payload instanceof String str) {
            return str;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    @Transactional(readOnly = true)
    public List<JobResponse> list(UUID projectId, UUID userId, int limit) {
        authorizationService.requireProjectAccess(projectId, userId);
        int size = limit <= 0 ? 20 : Math.min(limit, 50);
        return jobRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, size)).stream()
                .map(BackgroundJobService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse get(UUID jobId, UUID userId) {
        BackgroundJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Job not found"));
        authorizationService.requireProjectAccess(job.getProjectId(), userId);
        return toResponse(job);
    }

    static JobResponse toResponse(BackgroundJobEntity job) {
        return new JobResponse(
                job.getId(),
                job.getProjectId(),
                job.getJobType().name(),
                job.getStatus().name(),
                job.getPayload(),
                job.getResult(),
                job.getErrorMessage(),
                job.getAttempts(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
