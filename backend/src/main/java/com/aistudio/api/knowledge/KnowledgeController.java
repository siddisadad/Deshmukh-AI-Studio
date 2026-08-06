package com.aistudio.api.knowledge;

import com.aistudio.api.knowledge.dto.KnowledgeReindexResponse;
import com.aistudio.api.knowledge.dto.KnowledgeSearchResponse;
import com.aistudio.api.knowledge.dto.KnowledgeStatusResponse;
import com.aistudio.application.knowledge.KnowledgeService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Knowledge / RAG")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/api/v1/projects/{projectId}/knowledge")
    @Operation(summary = "Knowledge index status for a project")
    public KnowledgeStatusResponse status(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return knowledgeService.status(projectId, user.getId());
    }

    @PostMapping("/api/v1/projects/{projectId}/knowledge/reindex")
    @Operation(summary = "Rebuild the project knowledge index")
    public KnowledgeReindexResponse reindex(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return knowledgeService.reindex(projectId, user.getId());
    }

    @GetMapping("/api/v1/projects/{projectId}/knowledge/search")
    @Operation(summary = "Semantic search over indexed project knowledge")
    public KnowledgeSearchResponse search(
            @PathVariable UUID projectId,
            @RequestParam String q,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return knowledgeService.search(projectId, user.getId(), q, limit);
    }
}
