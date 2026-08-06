package com.aistudio.application.document;

import com.aistudio.api.document.dto.CreateDocumentRequest;
import com.aistudio.api.document.dto.DocumentAiResponse;
import com.aistudio.api.document.dto.DocumentResponse;
import com.aistudio.api.document.dto.GenerateDocumentRequest;
import com.aistudio.api.document.dto.UpdateDocumentRequest;
import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.application.ai.ContextBuilder;
import com.aistudio.application.ai.PromptTemplateManager;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.document.DocumentType;
import com.aistudio.infrastructure.persistence.entity.DocumentEntity;
import com.aistudio.infrastructure.persistence.repository.DocumentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ContextBuilder contextBuilder;
    private final PromptTemplateManager promptTemplateManager;
    private final AiProviderPort aiProviderPort;

    public DocumentService(
            DocumentRepository documentRepository,
            ProjectAuthorizationService authorizationService,
            ContextBuilder contextBuilder,
            PromptTemplateManager promptTemplateManager,
            AiProviderPort aiProviderPort
    ) {
        this.documentRepository = documentRepository;
        this.authorizationService = authorizationService;
        this.contextBuilder = contextBuilder;
        this.promptTemplateManager = promptTemplateManager;
        this.aiProviderPort = aiProviderPort;
    }

    @Transactional
    public DocumentResponse create(UUID projectId, UUID userId, CreateDocumentRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        DocumentEntity entity = new DocumentEntity();
        entity.setProjectId(projectId);
        entity.setTitle(request.title().trim());
        entity.setDocType(request.docType() == null ? DocumentType.OTHER : request.docType());
        entity.setContentMd(request.contentMd() == null ? "" : request.contentMd());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        documentRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID projectId, UUID userId) {
        authorizationService.requireProjectAccess(projectId, userId);
        return documentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(DocumentService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(UUID documentId, UUID userId) {
        DocumentEntity entity = requireReadable(documentId, userId);
        return toResponse(entity);
    }

    @Transactional
    public DocumentResponse update(UUID documentId, UUID userId, UpdateDocumentRequest request) {
        DocumentEntity entity = requireEditable(documentId, userId);
        if (request.title() != null && !request.title().isBlank()) {
            entity.setTitle(request.title().trim());
        }
        if (request.docType() != null) {
            entity.setDocType(request.docType());
        }
        if (request.contentMd() != null) {
            entity.setContentMd(request.contentMd());
        }
        entity.setUpdatedBy(userId);
        documentRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public void delete(UUID documentId, UUID userId) {
        DocumentEntity entity = requireEditable(documentId, userId);
        documentRepository.delete(entity);
    }

    @Transactional
    public DocumentAiResponse generate(UUID documentId, UUID userId, GenerateDocumentRequest request) {
        DocumentEntity entity = requireEditable(documentId, userId);
        String context = contextBuilder.buildForProject(entity.getProjectId(), entity.getTitle() + " " + nullToEmpty(entity.getContentMd()));
        String system = promptTemplateManager.systemPrompt("documentation_writer");
        String userPrompt = promptTemplateManager.actionPrompt("docs_generate", Map.of(
                "project_context", context,
                "doc_type", entity.getDocType().name(),
                "title", entity.getTitle(),
                "content_md", nullToEmpty(entity.getContentMd()),
                "instructions", request == null || request.instructions() == null ? "" : request.instructions()
        ));

        AiProviderPort.AiGenerationResult result = aiProviderPort.generate(new AiProviderPort.AiGenerationRequest(
                system,
                List.of(new AiProviderPort.AiMessage("user", userPrompt)),
                0.2,
                3000,
                Map.of("action", "docs_generate")
        ));

        entity.setContentMd(result.text());
        entity.setUpdatedBy(userId);
        documentRepository.save(entity);

        return new DocumentAiResponse(
                toResponse(entity),
                "DOCUMENTATION_WRITER",
                aiProviderPort.providerId(),
                result.model(),
                result.text()
        );
    }

    private DocumentEntity requireReadable(UUID documentId, UUID userId) {
        DocumentEntity entity = documentRepository.findById(documentId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Document not found"));
        authorizationService.requireProjectAccess(entity.getProjectId(), userId);
        return entity;
    }

    private DocumentEntity requireEditable(UUID documentId, UUID userId) {
        DocumentEntity entity = documentRepository.findById(documentId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Document not found"));
        authorizationService.requireProjectEdit(entity.getProjectId(), userId);
        return entity;
    }

    private static DocumentResponse toResponse(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getTitle(),
                entity.getDocType().name(),
                entity.getContentMd(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
