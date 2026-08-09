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
}
