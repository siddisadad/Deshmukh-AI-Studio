package com.aistudio.application.job;

import com.aistudio.application.document.DocumentService;
import com.aistudio.application.knowledge.KnowledgeIndexService;
import com.aistudio.domain.job.JobStatus;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.persistence.entity.BackgroundJobEntity;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "aistudio.jobs.worker-enabled", havingValue = "true", matchIfMissing = true)
public class BackgroundJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobWorker.class);

    private final BackgroundJobRepository jobRepository;
    private final BackgroundJobClaimer claimer;
    private final WorkerIdentity workerIdentity;
    private final KnowledgeIndexService knowledgeIndexService;
    private final DocumentService documentService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final int staleLockSeconds;
    private final int maxAttempts;

    public BackgroundJobWorker(
            BackgroundJobRepository jobRepository,
            BackgroundJobClaimer claimer,
            WorkerIdentity workerIdentity,
            KnowledgeIndexService knowledgeIndexService,
            DocumentService documentService,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${aistudio.jobs.batch-size:5}") int batchSize,
            @org.springframework.beans.factory.annotation.Value("${aistudio.jobs.stale-lock-seconds:900}") int staleLockSeconds,
            @org.springframework.beans.factory.annotation.Value("${aistudio.jobs.max-attempts:3}") int maxAttempts
    ) {
        this.jobRepository = jobRepository;
        this.claimer = claimer;
        this.workerIdentity = workerIdentity;
        this.knowledgeIndexService = knowledgeIndexService;
        this.documentService = documentService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.staleLockSeconds = staleLockSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${aistudio.jobs.poll-interval-ms:2000}")
    public void poll() {
        List<UUID> claimed = transactionTemplate.execute(status ->
                claimer.claimNext(batchSize, workerIdentity.id()));
        if (claimed == null || claimed.isEmpty()) {
            return;
        }
        for (UUID jobId : claimed) {
            processClaimed(jobId);
        }
    }

    @Scheduled(fixedDelayString = "${aistudio.jobs.stale-reclaim-interval-ms:60000}")
    public void recoverStaleLocks() {
        Integer reclaimed = transactionTemplate.execute(status ->
                claimer.reclaimStaleLocks(staleLockSeconds, maxAttempts));
        if (reclaimed != null && reclaimed > 0) {
            log.info("Reclaimed or failed {} stale background job lock(s)", reclaimed);
        }
    }

    public void processOne(UUID jobId) {
        boolean claimed = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                claimer.tryClaim(jobId, workerIdentity.id())));
        if (!claimed) {
            return;
        }
        processClaimed(jobId);
    }

    private void processClaimed(UUID jobId) {
        BackgroundJobEntity job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.RUNNING) {
            return;
        }

        try {
            String resultJson = execute(job);
            transactionTemplate.executeWithoutResult(status -> {
                BackgroundJobEntity current = jobRepository.findById(jobId).orElseThrow();
                current.setStatus(JobStatus.SUCCEEDED);
                current.setResult(resultJson);
                current.setFinishedAt(Instant.now());
                current.setLockedBy(null);
                current.setLockedAt(null);
                jobRepository.save(current);
            });
        } catch (Exception ex) {
            log.warn("Background job {} failed: {}", jobId, ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> {
                BackgroundJobEntity current = jobRepository.findById(jobId).orElseThrow();
                current.setStatus(JobStatus.FAILED);
                current.setErrorMessage(ex.getMessage() == null ? "Job failed" : truncate(ex.getMessage(), 2000));
                current.setFinishedAt(Instant.now());
                current.setLockedBy(null);
                current.setLockedAt(null);
                jobRepository.save(current);
            });
        }
    }

    private String execute(BackgroundJobEntity job) throws Exception {
        return switch (job.getJobType()) {
            case KNOWLEDGE_REINDEX -> {
                KnowledgeIndexService.ReindexResult result = knowledgeIndexService.reindexProject(job.getProjectId());
                yield objectMapper.writeValueAsString(Map.of(
                        "chunkCount", result.chunkCount(),
                        "embeddingProvider", result.embeddingProvider(),
                        "enabled", result.enabled(),
                        "maxChunksPerProject", result.maxChunksPerProject(),
                        "corpusLimitReached", result.corpusLimitReached()
                ));
            }
            case DOCUMENT_GENERATE -> {
                JsonNode payload = objectMapper.readTree(job.getPayload() == null ? "{}" : job.getPayload());
                UUID documentId = UUID.fromString(payload.path("documentId").asText());
                String instructions = payload.path("instructions").asText(null);
                UUID userId = job.getCreatedBy();
                var response = documentService.generate(
                        documentId,
                        userId,
                        new com.aistudio.api.document.dto.GenerateDocumentRequest(instructions)
                );
                yield objectMapper.writeValueAsString(Map.of(
                        "documentId", response.document().id().toString(),
                        "provider", response.provider(),
                        "model", response.model(),
                        "assistantRole", response.assistantRole()
                ));
            }
        };
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
