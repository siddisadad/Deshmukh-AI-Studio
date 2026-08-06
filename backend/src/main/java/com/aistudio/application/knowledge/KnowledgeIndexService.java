package com.aistudio.application.knowledge;

import com.aistudio.domain.knowledge.KnowledgeSourceType;
import com.aistudio.infrastructure.config.AiProperties;
import com.aistudio.infrastructure.knowledge.KnowledgeChunkJdbcRepository;
import com.aistudio.infrastructure.persistence.entity.ContextAssetEntity;
import com.aistudio.infrastructure.persistence.entity.DocumentEntity;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.entity.TaskEntity;
import com.aistudio.infrastructure.persistence.repository.ContextAssetRepository;
import com.aistudio.infrastructure.persistence.repository.DocumentRepository;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import com.aistudio.infrastructure.persistence.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeIndexService {

    private static final int CHUNK_SIZE = 900;
    private static final int CHUNK_OVERLAP = 120;

    private final EmbeddingPort embeddingPort;
    private final KnowledgeChunkJdbcRepository chunkRepository;
    private final RequirementRepository requirementRepository;
    private final DocumentRepository documentRepository;
    private final ContextAssetRepository contextAssetRepository;
    private final TaskRepository taskRepository;
    private final boolean ragEnabled;

    public KnowledgeIndexService(
            EmbeddingPort embeddingPort,
            KnowledgeChunkJdbcRepository chunkRepository,
            RequirementRepository requirementRepository,
            DocumentRepository documentRepository,
            ContextAssetRepository contextAssetRepository,
            TaskRepository taskRepository,
            AiProperties aiProperties
    ) {
        this.embeddingPort = embeddingPort;
        this.chunkRepository = chunkRepository;
        this.requirementRepository = requirementRepository;
        this.documentRepository = documentRepository;
        this.contextAssetRepository = contextAssetRepository;
        this.taskRepository = taskRepository;
        this.ragEnabled = aiProperties.rag() == null || aiProperties.rag().enabled();
    }

    @Transactional
    public ReindexResult reindexProject(UUID projectId) {
        if (!ragEnabled) {
            return new ReindexResult(0, embeddingPort.providerId(), false);
        }
        chunkRepository.deleteByProjectId(projectId);
        int chunks = 0;
        for (RequirementEntity req : requirementRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)) {
            chunks += indexText(
                    projectId,
                    KnowledgeSourceType.REQUIREMENT,
                    req.getId(),
                    req.getTitle(),
                    req.getTitle() + "\n" + nullToEmpty(req.getDescription())
            );
        }
        for (DocumentEntity doc : documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)) {
            chunks += indexText(
                    projectId,
                    KnowledgeSourceType.DOCUMENT,
                    doc.getId(),
                    doc.getTitle(),
                    doc.getTitle() + "\n" + nullToEmpty(doc.getContentMd())
            );
        }
        for (ContextAssetEntity asset : contextAssetRepository.findByProjectIdOrderByAssetTypeAsc(projectId)) {
            chunks += indexText(
                    projectId,
                    KnowledgeSourceType.CONTEXT_ASSET,
                    asset.getId(),
                    asset.getTitle(),
                    asset.getAssetType() + " " + asset.getTitle() + "\n" + nullToEmpty(asset.getContent())
            );
        }
        for (TaskEntity task : taskRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)) {
            chunks += indexText(
                    projectId,
                    KnowledgeSourceType.TASK,
                    task.getId(),
                    task.getTitle(),
                    task.getTitle() + "\n" + nullToEmpty(task.getDescription())
            );
        }
        return new ReindexResult(chunks, embeddingPort.providerId(), true);
    }

    @Transactional
    public void reindexSource(UUID projectId, KnowledgeSourceType sourceType, UUID sourceId, String title, String body) {
        if (!ragEnabled) {
            return;
        }
        chunkRepository.deleteBySource(projectId, sourceType, sourceId);
        indexText(projectId, sourceType, sourceId, title, title + "\n" + nullToEmpty(body));
    }

    private int indexText(
            UUID projectId,
            KnowledgeSourceType sourceType,
            UUID sourceId,
            String title,
            String body
    ) {
        List<String> chunks = chunk(body);
        if (chunks.isEmpty()) {
            return 0;
        }
        List<String> titles = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            titles.add(title == null || title.isBlank() ? sourceType.name() : title);
        }
        List<float[]> embeddings = embeddingPort.embedAll(chunks);
        chunkRepository.insertChunks(projectId, sourceType, sourceId, titles, chunks, embeddings);
        return chunks.size();
    }

    static List<String> chunk(String text) {
        String normalized = nullToEmpty(text).trim().replace("\r\n", "\n");
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= CHUNK_SIZE) {
            return List.of(normalized);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            if (end < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', end);
                if (breakAt <= start + CHUNK_SIZE / 2) {
                    breakAt = normalized.lastIndexOf(' ', end);
                }
                if (breakAt > start + CHUNK_SIZE / 2) {
                    end = breakAt;
                }
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isBlank()) {
                chunks.add(piece);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record ReindexResult(int chunkCount, String embeddingProvider, boolean enabled) {
    }
}
