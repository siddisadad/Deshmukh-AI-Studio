package com.aistudio.application.context;

import com.aistudio.api.context.dto.ContextAssetResponse;
import com.aistudio.api.context.dto.UpsertContextAssetRequest;
import com.aistudio.application.knowledge.KnowledgeIndexService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.context.ContextAssetType;
import com.aistudio.domain.knowledge.KnowledgeSourceType;
import com.aistudio.infrastructure.persistence.entity.ContextAssetEntity;
import com.aistudio.infrastructure.persistence.repository.ContextAssetRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContextAssetService {

    private final ContextAssetRepository contextAssetRepository;
    private final ProjectAuthorizationService authorizationService;
    private final KnowledgeIndexService knowledgeIndexService;

    public ContextAssetService(
            ContextAssetRepository contextAssetRepository,
            ProjectAuthorizationService authorizationService,
            KnowledgeIndexService knowledgeIndexService
    ) {
        this.contextAssetRepository = contextAssetRepository;
        this.authorizationService = authorizationService;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    @Transactional(readOnly = true)
    public List<ContextAssetResponse> list(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return contextAssetRepository.findByProjectIdOrderByAssetTypeAsc(projectId).stream()
                .map(ContextAssetService::toResponse)
                .toList();
    }

    @Transactional
    public ContextAssetResponse upsert(UUID projectId, String assetTypeValue, UUID userId, UpsertContextAssetRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        ContextAssetType assetType = parseType(assetTypeValue);
        ContextAssetEntity entity = contextAssetRepository.findByProjectIdAndAssetType(projectId, assetType)
                .orElseGet(() -> {
                    ContextAssetEntity created = new ContextAssetEntity();
                    created.setProjectId(projectId);
                    created.setAssetType(assetType);
                    return created;
                });
        entity.setTitle(request.title().trim());
        entity.setContent(request.content() == null ? "" : request.content());
        entity.setMetadata(request.metadata() == null || request.metadata().isBlank() ? "{}" : request.metadata());
        contextAssetRepository.save(entity);
        knowledgeIndexService.reindexSource(
                projectId,
                KnowledgeSourceType.CONTEXT_ASSET,
                entity.getId(),
                entity.getTitle(),
                entity.getContent()
        );
        return toResponse(entity);
    }

    private static ContextAssetType parseType(String value) {
        try {
            return ContextAssetType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Invalid context asset type");
        }
    }

    private static ContextAssetResponse toResponse(ContextAssetEntity entity) {
        return new ContextAssetResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getAssetType().name(),
                entity.getTitle(),
                entity.getContent(),
                entity.getMetadata(),
                entity.getUpdatedAt()
        );
    }
}
