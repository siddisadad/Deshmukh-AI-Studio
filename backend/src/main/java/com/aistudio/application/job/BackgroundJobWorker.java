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
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "aistudio.jobs.worker-enabled", havingValue = "true", matchIfMissing = true)
public class BackgroundJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobWorker.class);

    private final BackgroundJobRepository jobRepository;
    private final KnowledgeIndexService knowledgeIndexService;
    private final DocumentService documentService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public BackgroundJobWorker(
            BackgroundJobRepository jobRepository,
            KnowledgeIndexService knowledgeIndexService,
            DocumentService documentService,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.knowledgeIndexService = knowledgeIndexService;
        this.documentService = documentService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${aistudio.jobs.poll-interval-ms:2000}")
    public void poll() {
        List<BackgroundJobEntity> pending = jobRepository.findPending(PageRequest.of(0, 5));
        for (BackgroundJobEntity job : pending) {
            processOne(job.getId());
        }
    }

    public void processOne(UUID jobId) {
        BackgroundJobEntity claimed = transactionTemplate.execute(status -> {
            BackgroundJobEntity job = jobRepository.findById(jobId).orElse(null);
            if (job == null || job.getStatus() != JobStatus.PENDING) {
                return null;
            }
            job.setStatus(JobStatus.RUNNING);
            job.setAttempts(job.getAttempts() + 1);
            job.setStartedAt(Instant.now());
            job.setErrorMessage(null);
            return jobRepository.save(job);
        });
        if (claimed == null) {
            return;
        }

        try {
            String resultJson = execute(claimed);
            transactionTemplate.executeWithoutResult(status -> {
                BackgroundJobEntity job = jobRepository.findById(jobId).orElseThrow();
                job.setStatus(JobStatus.SUCCEEDED);
                job.setResult(resultJson);
                job.setFinishedAt(Instant.now());
                jobRepository.save(job);
            });
        } catch (Exception ex) {
            log.warn("Background job {} failed: {}", jobId, ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> {
                BackgroundJobEntity job = jobRepository.findById(jobId).orElseThrow();
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage(ex.getMessage() == null ? "Job failed" : truncate(ex.getMessage(), 2000));
                job.setFinishedAt(Instant.now());
                jobRepository.save(job);
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
                        "enabled", result.enabled()
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
