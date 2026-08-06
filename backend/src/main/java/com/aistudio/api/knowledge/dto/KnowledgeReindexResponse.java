package com.aistudio.api.knowledge.dto;

public record KnowledgeReindexResponse(
        int chunkCount,
        String embeddingProvider,
        boolean enabled
) {
}
