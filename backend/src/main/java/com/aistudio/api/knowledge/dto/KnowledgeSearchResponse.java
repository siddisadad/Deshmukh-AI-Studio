package com.aistudio.api.knowledge.dto;

import java.util.List;
import java.util.UUID;

public record KnowledgeSearchResponse(
        String query,
        String embeddingProvider,
        int indexedChunks,
        List<Hit> hits
) {
    public record Hit(
            UUID id,
            String sourceType,
            UUID sourceId,
            String title,
            String content,
            double score
    ) {
    }
}
