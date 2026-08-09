package com.aistudio.application.knowledge;

import com.aistudio.domain.knowledge.KnowledgeSourceType;
import com.aistudio.infrastructure.config.AiProperties;
import com.aistudio.infrastructure.knowledge.KnowledgeChunkJdbcRepository;
import com.aistudio.infrastructure.metrics.KnowledgeEmbeddingMetrics;
import com.aistudio.infrastructure.persistence.entity.ContextAssetEntity;
import com.aistudio.infrastructure.persistence.entity.DocumentEntity;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.entity.TaskEntity;
import com.aistudio.infrastructure.persistence.repository.ContextAssetRepository;
import com.aistudio.infrastructure.persistence.repository.DocumentRepository;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import com.aistudio.infrastructure.persistence.entity.ProjectCodeFileEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectCodeFileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeIndexService {

    private final EmbeddingPort embeddingPort;
    private final KnowledgeChunkJdbcRepository chunkRepository;
    private final RequirementRepository requirementRepository;
    private final DocumentRepository documentRepository;
    private final ContextAssetRepository contextAssetRepository;
    private final TaskRepository taskRepository;
    private final ProjectCodeFileRepository codeFileRepository;
    private final KnowledgeEmbeddingMetrics embeddingMetrics;
    private final boolean ragEnabled;
    private final int maxChunksPerProject;
    private final int chunkSize;
    private final int chunkOverlap;

    public KnowledgeIndexService(
            EmbeddingPort embeddingPort,
            KnowledgeChunkJdbcRepository chunkRepository,
            RequirementRepository requirementRepository,
            DocumentRepository documentRepository,
            ContextAssetRepository contextAssetRepository,
            TaskRepository taskRepository,
            ProjectCodeFileRepository codeFileRepository,
            KnowledgeEmbeddingMetrics embeddingMetrics,
            AiProperties aiProperties
    ) {
        this.embeddingPort = embeddingPort;
        this.chunkRepository = chunkRepository;
        this.requirementRepository = requirementRepository;
        this.documentRepository = documentRepository;
        this.contextAssetRepository = contextAssetRepository;
        this.taskRepository = taskRepository;
        this.codeFileRepository = codeFileRepository;
        this.embeddingMetrics = embeddingMetrics;
        this.ragEnabled = aiProperties.rag() == null || aiProperties.rag().enabled();
        this.maxChunksPerProject = aiProperties.rag() == null || aiProperties.rag().maxChunksPerProject() <= 0
                ? 10000
                : aiProperties.rag().maxChunksPerProject();
        this.chunkSize = aiProperties.rag() == null || aiProperties.rag().chunkSize() <= 0
                ? 900
                : aiProperties.rag().chunkSize();
        this.chunkOverlap = aiProperties.rag() == null || aiProperties.rag().chunkOverlap() < 0
                ? 120
                : aiProperties.rag().chunkOverlap();
    }

    @Transactional
    public ReindexResult reindexProject(UUID projectId) {
        if (!ragEnabled) {
            return new ReindexResult(0, embeddingPort.providerId(), false, maxChunksPerProject, false);
        }
        chunkRepository.deleteByProjectId(projectId);
        int chunks = 0;
        boolean corpusLimitReached = false;
        for (RequirementEntity req : requirementRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)) {
            IndexSlice slice = indexText(
                    projectId,
                    KnowledgeSourceType.REQUIREMENT,
                    req.getId(),
                    req.getTitle(),
                    req.getTitle() + "\n" + nullToEmpty(req.getDescription()),
                    chunks
            );
            chunks += slice.chunksIndexed();
            if (slice.corpusLimitReached()) {
                corpusLimitReached = true;
                break;
            }
        }
        if (!corpusLimitReached) {
            for (DocumentEntity doc : documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)) {
                IndexSlice slice = indexText(
                        projectId,
                        KnowledgeSourceType.DOCUMENT,
                        doc.getId(),
                        doc.getTitle(),
                        doc.getTitle() + "\n" + nullToEmpty(doc.getContentMd()),
                        chunks
                );
                chunks += slice.chunksIndexed();
                if (slice.corpusLimitReached()) {
                    corpusLimitReached = true;
                    break;
                }
            }
        }
        if (!corpusLimitReached) {
            for (ContextAssetEntity asset : contextAssetRepository.findByProjectIdOrderByAssetTypeAsc(projectId)) {
                IndexSlice slice = indexText(
                        projectId,
                        KnowledgeSourceType.CONTEXT_ASSET,
                        asset.getId(),
                        asset.getTitle(),
                        asset.getAssetType() + " " + asset.getTitle() + "\n" + nullToEmpty(asset.getContent()),
                        chunks
                );
                chunks += slice.chunksIndexed();
                if (slice.corpusLimitReached()) {
                    corpusLimitReached = true;
                    break;
                }
            }
        }
        if (!corpusLimitReached) {
            for (TaskEntity task : taskRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)) {
                IndexSlice slice = indexText(
                        projectId,
                        KnowledgeSourceType.TASK,
                        task.getId(),
                        task.getTitle(),
                        task.getTitle() + "\n" + nullToEmpty(task.getDescription()),
                        chunks
                );
                chunks += slice.chunksIndexed();
                if (slice.corpusLimitReached()) {
                    corpusLimitReached = true;
                    break;
                }
            }
        }
        if (!corpusLimitReached) {
            for (ProjectCodeFileEntity file : codeFileRepository.findByProjectIdOrderByPathAsc(projectId)) {
                IndexSlice slice = indexCodeFile(projectId, file, chunks);
                chunks += slice.chunksIndexed();
                if (slice.corpusLimitReached()) {
                    corpusLimitReached = true;
                    break;
                }
            }
        }
        return new ReindexResult(
                chunks,
                embeddingPort.providerId(),
                true,
                maxChunksPerProject,
                corpusLimitReached
        );
    }

    @Transactional
    public void reindexCodeFiles(UUID projectId) {
        if (!ragEnabled) {
            return;
        }
        chunkRepository.deleteBySourceType(projectId, KnowledgeSourceType.CODE_FILE);
        int chunks = chunkRepository.countByProjectId(projectId);
        for (ProjectCodeFileEntity file : codeFileRepository.findByProjectIdOrderByPathAsc(projectId)) {
            IndexSlice slice = indexCodeFile(projectId, file, chunks);
            chunks += slice.chunksIndexed();
            if (slice.corpusLimitReached()) {
                break;
            }
        }
    }

    @Transactional
    public void reindexSource(UUID projectId, KnowledgeSourceType sourceType, UUID sourceId, String title, String body) {
        if (!ragEnabled) {
            return;
        }
        chunkRepository.deleteBySource(projectId, sourceType, sourceId);
        int existing = chunkRepository.countByProjectId(projectId);
        indexText(projectId, sourceType, sourceId, title, title + "\n" + nullToEmpty(body), existing);
    }

    private IndexSlice indexCodeFile(UUID projectId, ProjectCodeFileEntity file, int existingChunks) {
        String title = file.getPath();
        String body = "path: " + file.getPath()
                + "\nlanguage: " + nullToEmpty(file.getLanguage())
                + "\nsize_bytes: " + file.getSizeBytes()
                + "\n" + nullToEmpty(file.getSnippet());
        return indexText(
                projectId,
                KnowledgeSourceType.CODE_FILE,
                file.getId(),
                title,
                body,
                existingChunks
        );
    }

    private IndexSlice indexText(
            UUID projectId,
            KnowledgeSourceType sourceType,
            UUID sourceId,
            String title,
            String body,
            int existingChunks
    ) {
        if (existingChunks >= maxChunksPerProject) {
            return new IndexSlice(0, true);
        }
        List<String> chunks = chunk(body, chunkSize, chunkOverlap);
        if (chunks.isEmpty()) {
            return new IndexSlice(0, false);
        }
        int remaining = maxChunksPerProject - existingChunks;
        boolean truncated = chunks.size() > remaining;
        if (truncated) {
            chunks = chunks.subList(0, remaining);
        }
        List<String> titles = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            titles.add(title == null || title.isBlank() ? sourceType.name() : title);
        }
        List<float[]> embeddings = embeddingPort.embedAll(chunks);
        embeddingMetrics.recordEmbeddings(embeddings.size());
        chunkRepository.insertChunks(projectId, sourceType, sourceId, titles, chunks, embeddings);
        return new IndexSlice(chunks.size(), truncated);
    }

    static List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        String normalized = nullToEmpty(text).trim().replace("\r\n", "\n");
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= chunkSize) {
            return List.of(normalized);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + chunkSize);
            if (end < normalized.length()) {
                int breakAt = normalized.lastIndexOf('\n', end);
                if (breakAt <= start + chunkSize / 2) {
                    breakAt = normalized.lastIndexOf(' ', end);
                }
                if (breakAt > start + chunkSize / 2) {
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
            start = Math.max(end - chunkOverlap, start + 1);
        }
        return chunks;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public int maxChunksPerProject() {
        return maxChunksPerProject;
    }

    private record IndexSlice(int chunksIndexed, boolean corpusLimitReached) {
    }

    public record ReindexResult(
            int chunkCount,
            String embeddingProvider,
            boolean enabled,
            int maxChunksPerProject,
            boolean corpusLimitReached
    ) {
    }
}
