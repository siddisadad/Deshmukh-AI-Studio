package com.aistudio.api.codemetadata;

import com.aistudio.application.codemetadata.ProjectGitSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Git webhook")
public class GitWebhookController {

    private final ProjectGitSyncService gitSyncService;

    public GitWebhookController(ProjectGitSyncService gitSyncService) {
        this.gitSyncService = gitSyncService;
    }

    @PostMapping("/api/v1/git/webhook/github/{projectId}")
    @Operation(summary = "GitHub push webhook — enqueues code metadata sync (no JWT)")
    public ResponseEntity<Void> github(
            @PathVariable UUID projectId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload
    ) {
        gitSyncService.handleGithubWebhook(projectId, signature, payload == null ? "" : payload);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/v1/git/webhook/gitlab/{projectId}")
    @Operation(summary = "GitLab push webhook — enqueues code metadata sync (no JWT)")
    public ResponseEntity<Void> gitlab(
            @PathVariable UUID projectId,
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestBody String payload
    ) {
        gitSyncService.handleGitlabWebhook(projectId, token, payload == null ? "" : payload);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/v1/git/webhook/bitbucket/{projectId}")
    @Operation(summary = "Bitbucket push webhook — enqueues code metadata sync (no JWT)")
    public ResponseEntity<Void> bitbucket(
            @PathVariable UUID projectId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload
    ) {
        gitSyncService.handleBitbucketWebhook(projectId, signature, payload == null ? "" : payload);
        return ResponseEntity.accepted().build();
    }
}
