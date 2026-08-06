package com.aistudio.api.context;

import com.aistudio.api.context.dto.ContextAssetResponse;
import com.aistudio.api.context.dto.UpsertContextAssetRequest;
import com.aistudio.application.context.ContextAssetService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Context Assets")
public class ContextAssetController {

    private final ContextAssetService contextAssetService;

    public ContextAssetController(ContextAssetService contextAssetService) {
        this.contextAssetService = contextAssetService;
    }

    @GetMapping("/api/v1/projects/{projectId}/context-assets")
    @Operation(summary = "List project context assets")
    public List<ContextAssetResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return contextAssetService.list(projectId, user.getId());
    }

    @PutMapping("/api/v1/projects/{projectId}/context-assets/{assetType}")
    @Operation(summary = "Upsert a context asset by type")
    public ContextAssetResponse upsert(
            @PathVariable UUID projectId,
            @PathVariable String assetType,
            @Valid @RequestBody UpsertContextAssetRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return contextAssetService.upsert(projectId, assetType, user.getId(), request);
    }
}
