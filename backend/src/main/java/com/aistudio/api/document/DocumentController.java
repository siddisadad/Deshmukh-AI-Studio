package com.aistudio.api.document;

import com.aistudio.api.document.dto.CreateDocumentRequest;
import com.aistudio.api.document.dto.DocumentAiResponse;
import com.aistudio.api.document.dto.DocumentResponse;
import com.aistudio.api.document.dto.GenerateDocumentRequest;
import com.aistudio.api.document.dto.UpdateDocumentRequest;
import com.aistudio.api.job.dto.JobResponse;
import com.aistudio.application.document.DocumentService;
import com.aistudio.application.job.BackgroundJobService;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentService documentService;
    private final BackgroundJobService backgroundJobService;
    private final ObjectMapper objectMapper;

    public DocumentController(
            DocumentService documentService,
            BackgroundJobService backgroundJobService,
            ObjectMapper objectMapper
    ) {
        this.documentService = documentService;
        this.backgroundJobService = backgroundJobService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/v1/projects/{projectId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create document")
    public DocumentResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return documentService.create(projectId, user.getId(), request);
    }

    @GetMapping("/api/v1/projects/{projectId}/documents")
    @Operation(summary = "List documents")
    public List<DocumentResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return documentService.list(projectId, user.getId());
    }

    @GetMapping("/api/v1/documents/{documentId}")
    @Operation(summary = "Get document")
    public DocumentResponse get(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return documentService.get(documentId, user.getId());
    }

    @PatchMapping("/api/v1/documents/{documentId}")
    @Operation(summary = "Update document")
    public DocumentResponse update(
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return documentService.update(documentId, user.getId(), request);
    }

    @DeleteMapping("/api/v1/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete document")
    public void delete(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        documentService.delete(documentId, user.getId());
    }

    @PostMapping("/api/v1/documents/{documentId}/ai/generate")
    @Operation(summary = "Generate document content with Documentation Writer")
    public DocumentAiResponse generate(
            @PathVariable UUID documentId,
            @RequestBody(required = false) @Valid GenerateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return documentService.generate(documentId, user.getId(), request);
    }

    @PostMapping("/api/v1/documents/{documentId}/ai/generate/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Enqueue document generation as a background job")
    public JobResponse generateAsync(
            @PathVariable UUID documentId,
            @RequestBody(required = false) @Valid GenerateDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) throws Exception {
        var document = documentService.get(documentId, user.getId());
        String payload = objectMapper.writeValueAsString(Map.of(
                "documentId", documentId.toString(),
                "instructions", request == null || request.instructions() == null ? "" : request.instructions()
        ));
        return backgroundJobService.enqueue(document.projectId(), user.getId(), JobType.DOCUMENT_GENERATE, payload);
    }
}
