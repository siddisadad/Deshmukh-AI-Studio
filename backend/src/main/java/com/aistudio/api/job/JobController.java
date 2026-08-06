package com.aistudio.api.job;

import com.aistudio.api.job.dto.JobResponse;
import com.aistudio.application.job.BackgroundJobService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Background Jobs")
public class JobController {

    private final BackgroundJobService backgroundJobService;

    public JobController(BackgroundJobService backgroundJobService) {
        this.backgroundJobService = backgroundJobService;
    }

    @GetMapping("/api/v1/projects/{projectId}/jobs")
    @Operation(summary = "List recent background jobs for a project")
    public List<JobResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return backgroundJobService.list(projectId, user.getId(), limit);
    }

    @GetMapping("/api/v1/jobs/{jobId}")
    @Operation(summary = "Get a background job by id")
    public JobResponse get(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return backgroundJobService.get(jobId, user.getId());
    }
}
