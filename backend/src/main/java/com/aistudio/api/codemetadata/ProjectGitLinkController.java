package com.aistudio.api.codemetadata;

import com.aistudio.api.codemetadata.dto.ProjectGitLinkResponse;
import com.aistudio.api.codemetadata.dto.UpsertProjectGitLinkRequest;
import com.aistudio.api.job.dto.JobResponse;
import com.aistudio.application.codemetadata.ProjectGitSyncService;
import com.aistudio.application.job.BackgroundJobService;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Git link")
public class ProjectGitLinkController {

    private final ProjectGitSyncService gitSyncService;
    private final BackgroundJobService backgroundJobService;

    public ProjectGitLinkController(
            ProjectGitSyncService gitSyncService,
            BackgroundJobService backgroundJobService
    ) {
        this.gitSyncService = gitSyncService;
        this.backgroundJobService = backgroundJobService;
    }

    @GetMapping("/api/v1/projects/{projectId}/git-link")
    @Operation(summary = "Git repository link for code metadata sync")
    public ProjectGitLinkResponse get(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return gitSyncService.getLink(projectId, user.getId());
    }

    @PutMapping("/api/v1/projects/{projectId}/git-link")
    @Operation(summary = "Configure Git repository link and webhook secret")
    public ProjectGitLinkResponse upsert(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpsertProjectGitLinkRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return gitSyncService.upsertLink(projectId, user.getId(), request);
    }

    @PostMapping("/api/v1/projects/{projectId}/git-link/sync")
    @Operation(summary = "Sync code metadata from linked Git repository now")
    public ProjectGitLinkResponse syncNow(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return gitSyncService.syncNow(projectId, user.getId());
    }

    @PostMapping("/api/v1/projects/{projectId}/git-link/sync/async")
    @Operation(summary = "Enqueue background code metadata sync from Git")
    public JobResponse syncAsync(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return backgroundJobService.enqueue(projectId, user.getId(), JobType.CODE_METADATA_SYNC, "{\"source\":\"manual\"}");
    }
}
