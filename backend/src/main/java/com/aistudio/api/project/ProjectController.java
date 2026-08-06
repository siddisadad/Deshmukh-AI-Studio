package com.aistudio.api.project;

import com.aistudio.api.project.dto.CreateProjectRequest;
import com.aistudio.api.project.dto.ProjectResponse;
import com.aistudio.api.project.dto.UpdateProjectRequest;
import com.aistudio.application.project.ProjectService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/api/v1/organizations/{orgId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create project")
    public ProjectResponse create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest http
    ) {
        return projectService.create(orgId, user.getId(), request, clientIp(http));
    }

    @GetMapping("/api/v1/organizations/{orgId}/projects")
    @Operation(summary = "List organization projects")
    public List<ProjectResponse> list(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return projectService.listByOrg(orgId, user.getId(), status);
    }

    @GetMapping("/api/v1/projects/{projectId}")
    @Operation(summary = "Get project")
    public ProjectResponse get(@PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        return projectService.get(projectId, user.getId());
    }

    @PatchMapping("/api/v1/projects/{projectId}")
    @Operation(summary = "Update project")
    public ProjectResponse update(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return projectService.update(projectId, user.getId(), request);
    }

    @PostMapping("/api/v1/projects/{projectId}/archive")
    @Operation(summary = "Archive project")
    public ProjectResponse archive(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest http
    ) {
        return projectService.archive(projectId, user.getId(), clientIp(http));
    }

    @PostMapping("/api/v1/projects/{projectId}/unarchive")
    @Operation(summary = "Unarchive project")
    public ProjectResponse unarchive(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest http
    ) {
        return projectService.unarchive(projectId, user.getId(), clientIp(http));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
