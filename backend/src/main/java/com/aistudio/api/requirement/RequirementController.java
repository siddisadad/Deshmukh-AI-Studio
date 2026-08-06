package com.aistudio.api.requirement;

import com.aistudio.api.requirement.dto.AiActionRequest;
import com.aistudio.api.requirement.dto.CreateRequirementRequest;
import com.aistudio.api.requirement.dto.RequirementAiResponse;
import com.aistudio.api.requirement.dto.RequirementResponse;
import com.aistudio.api.requirement.dto.UpdateRequirementRequest;
import com.aistudio.application.requirement.RequirementAiService;
import com.aistudio.application.requirement.RequirementService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@Tag(name = "Requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final RequirementAiService requirementAiService;

    public RequirementController(RequirementService requirementService, RequirementAiService requirementAiService) {
        this.requirementService = requirementService;
        this.requirementAiService = requirementAiService;
    }

    @PostMapping("/api/v1/projects/{projectId}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create requirement")
    public RequirementResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateRequirementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementService.create(projectId, user.getId(), request);
    }

    @GetMapping("/api/v1/projects/{projectId}/requirements")
    @Operation(summary = "List requirements")
    public List<RequirementResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementService.list(projectId, user.getId());
    }

    @GetMapping("/api/v1/requirements/{requirementId}")
    @Operation(summary = "Get requirement")
    public RequirementResponse get(
            @PathVariable UUID requirementId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementService.get(requirementId, user.getId());
    }

    @PatchMapping("/api/v1/requirements/{requirementId}")
    @Operation(summary = "Update requirement")
    public RequirementResponse update(
            @PathVariable UUID requirementId,
            @Valid @RequestBody UpdateRequirementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementService.update(requirementId, user.getId(), request);
    }

    @DeleteMapping("/api/v1/requirements/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete requirement")
    public void delete(
            @PathVariable UUID requirementId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requirementService.delete(requirementId, user.getId());
    }

    @PostMapping("/api/v1/requirements/{requirementId}/ai/improve")
    @Operation(summary = "AI: improve requirement description")
    public RequirementAiResponse improve(
            @PathVariable UUID requirementId,
            @RequestBody(required = false) @Valid AiActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementAiService.improve(requirementId, user.getId(), request == null ? null : request.instructions());
    }

    @PostMapping("/api/v1/requirements/{requirementId}/ai/user-stories")
    @Operation(summary = "AI: generate user stories")
    public RequirementAiResponse userStories(
            @PathVariable UUID requirementId,
            @RequestBody(required = false) @Valid AiActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementAiService.userStories(requirementId, user.getId(), request == null ? null : request.instructions());
    }

    @PostMapping("/api/v1/requirements/{requirementId}/ai/acceptance-criteria")
    @Operation(summary = "AI: generate acceptance criteria")
    public RequirementAiResponse acceptanceCriteria(
            @PathVariable UUID requirementId,
            @RequestBody(required = false) @Valid AiActionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return requirementAiService.acceptanceCriteria(requirementId, user.getId(), request == null ? null : request.instructions());
    }
}
