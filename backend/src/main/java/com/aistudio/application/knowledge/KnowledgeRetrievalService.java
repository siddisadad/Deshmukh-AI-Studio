package com.aistudio.application.knowledge;

import com.aistudio.infrastructure.config.AiProperties;
import com.aistudio.infrastructure.knowledge.KnowledgeChunkJdbcRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {

    private final EmbeddingPort embeddingPort;
    private final KnowledgeChunkJdbcRepository chunkRepository;
    private final boolean enabled;
    private final int topK;
    private final int maxChars;

    public KnowledgeRetrievalService(
            EmbeddingPort embeddingPort,
            KnowledgeChunkJdbcRepository chunkRepository,
            AiProperties aiProperties
    ) {
        this.embeddingPort = embeddingPort;
        this.chunkRepository = chunkRepository;
        this.enabled = aiProperties.rag() == null || aiProperties.rag().enabled();
        this.topK = aiProperties.rag() == null || aiProperties.rag().topK() <= 0 ? 8 : aiProperties.rag().topK();
        this.maxChars = aiProperties.rag() == null || aiProperties.rag().maxChars() <= 0
                ? 6000
                : aiProperties.rag().maxChars();
    }

    public List<KnowledgeChunkHit> search(UUID projectId, String query, Integer limit) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }
        if (chunkRepository.countByProjectId(projectId) == 0) {
            return List.of();
        }
        int k = limit == null || limit <= 0 ? topK : Math.min(limit, 20);
        float[] embedding = embeddingPort.embed(query);
        return chunkRepository.search(projectId, embedding, k);
    }

    public String formatForPrompt(UUID projectId, String query) {
        List<KnowledgeChunkHit> hits = search(projectId, query, topK);
        if (hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Retrieved knowledge (RAG)\n");
        int used = 0;
        for (KnowledgeChunkHit hit : hits) {
            String block = "- [" + hit.sourceType() + "] " + hit.title()
                    + " (score=" + String.format("%.3f", hit.score()) + ")\n  "
                    + hit.content().replace("\n", " ") + "\n";
            if (used + block.length() > maxChars) {
                break;
            }
            sb.append(block);
            used += block.length();
        }
        return sb.toString();
    }

    public boolean enabled() {
        return enabled;
    }

    public String embeddingProviderId() {
        return embeddingPort.providerId();
    }

    public int indexedChunkCount(UUID projectId) {
        return chunkRepository.countByProjectId(projectId);
    }
}
