package com.aistudio.application.codemetadata;

import com.aistudio.api.codemetadata.dto.CodeFileResponse;
import com.aistudio.api.codemetadata.dto.CodeMetadataSummaryResponse;
import com.aistudio.api.codemetadata.dto.ReplaceCodeMetadataRequest;
import com.aistudio.application.knowledge.KnowledgeIndexService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.AiProperties;
import com.aistudio.infrastructure.persistence.entity.ProjectCodeFileEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectCodeFileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectCodeMetadataService {

    private final ProjectCodeFileRepository codeFileRepository;
    private final ProjectAuthorizationService authorizationService;
    private final KnowledgeIndexService knowledgeIndexService;
    private final int maxFilesPerProject;

    public ProjectCodeMetadataService(
            ProjectCodeFileRepository codeFileRepository,
            ProjectAuthorizationService authorizationService,
            KnowledgeIndexService knowledgeIndexService,
            AiProperties aiProperties
    ) {
        this.codeFileRepository = codeFileRepository;
        this.authorizationService = authorizationService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.maxFilesPerProject = aiProperties.rag() == null || aiProperties.rag().maxCodeFilesPerProject() <= 0
                ? 500
                : aiProperties.rag().maxCodeFilesPerProject();
    }

    @Transactional(readOnly = true)
    public CodeMetadataSummaryResponse summary(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return toSummary(projectId);
    }

    @Transactional
    public CodeMetadataSummaryResponse replaceManifest(
            UUID projectId,
            UUID userId,
            ReplaceCodeMetadataRequest request
    ) {
        authorizationService.requireProjectEdit(projectId, userId);
        List<ReplaceCodeMetadataRequest.CodeFileInput> files = request.files() == null ? List.of() : request.files();
        if (files.size() > maxFilesPerProject) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Manifest exceeds max code files per project (" + maxFilesPerProject + ")"
            );
        }
        codeFileRepository.deleteByProjectId(projectId);
        List<ProjectCodeFileEntity> saved = new ArrayList<>(files.size());
        for (ReplaceCodeMetadataRequest.CodeFileInput file : files) {
            String path = normalizePath(file.path());
            if (path.isBlank()) {
                continue;
            }
            ProjectCodeFileEntity entity = new ProjectCodeFileEntity();
            entity.setProjectId(projectId);
            entity.setPath(path);
            entity.setLanguage(file.language() == null ? "" : file.language().trim());
            entity.setSnippet(file.snippet() == null ? "" : file.snippet());
            entity.setSizeBytes(Math.max(0, file.sizeBytes()));
            saved.add(codeFileRepository.save(entity));
        }
        knowledgeIndexService.reindexCodeFiles(projectId);
        return toSummary(projectId);
    }

    private CodeMetadataSummaryResponse toSummary(UUID projectId) {
        List<CodeFileResponse> files = codeFileRepository.findByProjectIdOrderByPathAsc(projectId).stream()
                .map(ProjectCodeMetadataService::toResponse)
                .toList();
        return new CodeMetadataSummaryResponse(files.size(), maxFilesPerProject, files);
    }

    private static CodeFileResponse toResponse(ProjectCodeFileEntity entity) {
        return new CodeFileResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getPath(),
                entity.getLanguage(),
                entity.getSnippet(),
                entity.getSizeBytes(),
                entity.getUpdatedAt()
        );
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim().replace('\\', '/');
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }
}
