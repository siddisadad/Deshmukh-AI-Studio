package com.aistudio.application.knowledge;

import com.aistudio.api.knowledge.dto.KnowledgeReindexResponse;
import com.aistudio.api.knowledge.dto.KnowledgeSearchResponse;
import com.aistudio.api.knowledge.dto.KnowledgeStatusResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {

    private final ProjectAuthorizationService authorizationService;
    private final KnowledgeIndexService indexService;
    private final KnowledgeRetrievalService retrievalService;

    public KnowledgeService(
            ProjectAuthorizationService authorizationService,
            KnowledgeIndexService indexService,
            KnowledgeRetrievalService retrievalService
    ) {
        this.authorizationService = authorizationService;
        this.indexService = indexService;
        this.retrievalService = retrievalService;
    }

    @Transactional(readOnly = true)
    public KnowledgeStatusResponse status(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return new KnowledgeStatusResponse(
                retrievalService.enabled(),
                retrievalService.embeddingProviderId(),
                retrievalService.indexedChunkCount(projectId)
        );
    }

    @Transactional
    public KnowledgeReindexResponse reindex(UUID projectId, UUID userId) {
        authorizationService.requireProjectEdit(projectId, userId);
        KnowledgeIndexService.ReindexResult result = indexService.reindexProject(projectId);
        return new KnowledgeReindexResponse(result.chunkCount(), result.embeddingProvider(), result.enabled());
    }

    @Transactional(readOnly = true)
    public KnowledgeSearchResponse search(UUID projectId, UUID userId, String query, Integer limit) {
        authorizationService.requireProjectAccess(projectId, userId);
        var hits = retrievalService.search(projectId, query, limit).stream()
                .map(hit -> new KnowledgeSearchResponse.Hit(
                        hit.id(),
                        hit.sourceType().name(),
                        hit.sourceId(),
                        hit.title(),
                        hit.content(),
                        hit.score()
                ))
                .toList();
        return new KnowledgeSearchResponse(
                query,
                retrievalService.embeddingProviderId(),
                retrievalService.indexedChunkCount(projectId),
                hits
        );
    }
}
