package com.aistudio.api.knowledge.dto;

public record KnowledgeStatusResponse(
        boolean enabled,
        String embeddingProvider,
        int indexedChunks,
        int maxChunksPerProject,
        boolean corpusLimitReached
) {
}
