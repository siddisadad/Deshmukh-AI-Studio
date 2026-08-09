package com.aistudio.api.codemetadata;

import com.aistudio.api.codemetadata.dto.CodeMetadataSummaryResponse;
import com.aistudio.api.codemetadata.dto.ReplaceCodeMetadataRequest;
import com.aistudio.application.codemetadata.ProjectCodeMetadataService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Code Metadata")
public class CodeMetadataController {

    private final ProjectCodeMetadataService codeMetadataService;

    public CodeMetadataController(ProjectCodeMetadataService codeMetadataService) {
        this.codeMetadataService = codeMetadataService;
    }

    @GetMapping("/api/v1/projects/{projectId}/code-metadata")
    @Operation(summary = "List indexed code file metadata for a project")
    public CodeMetadataSummaryResponse list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return codeMetadataService.summary(projectId, user.getId());
    }

    @PutMapping("/api/v1/projects/{projectId}/code-metadata")
    @Operation(summary = "Replace the project code metadata manifest and reindex for RAG")
    public CodeMetadataSummaryResponse replace(
            @PathVariable UUID projectId,
            @Valid @RequestBody ReplaceCodeMetadataRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return codeMetadataService.replaceManifest(projectId, user.getId(), request);
    }
}
