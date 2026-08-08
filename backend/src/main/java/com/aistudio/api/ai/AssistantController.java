package com.aistudio.api.ai;

import com.aistudio.api.ai.dto.AiProviderHealthResponse;
import com.aistudio.api.ai.dto.AssistantsResponse;
import com.aistudio.api.ai.dto.ChatMessageResponse;
import com.aistudio.api.ai.dto.ConversationResponse;
import com.aistudio.api.ai.dto.ConversationShareResponse;
import com.aistudio.api.ai.dto.ConversationSummaryResponse;
import com.aistudio.api.ai.dto.CreateConversationRequest;
import com.aistudio.api.ai.dto.ExportedConversation;
import com.aistudio.api.ai.dto.SendMessageRequest;
import com.aistudio.api.ai.dto.RetentionPurgeRequest;
import com.aistudio.api.ai.dto.RetentionPurgeResponse;
import com.aistudio.api.ai.dto.UpdateConversationRequest;
import com.aistudio.application.ai.AiProviderHealthService;
import com.aistudio.application.ai.AssistantRegistry;
import com.aistudio.application.ai.ConversationService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Assistants")
public class AssistantController {

    private final AssistantRegistry assistantRegistry;
    private final ConversationService conversationService;
    private final AiProviderHealthService aiProviderHealthService;
    private final Executor sseTaskExecutor;

    public AssistantController(
            AssistantRegistry assistantRegistry,
            ConversationService conversationService,
            AiProviderHealthService aiProviderHealthService,
            @Qualifier("sseTaskExecutor") Executor sseTaskExecutor
    ) {
        this.assistantRegistry = assistantRegistry;
        this.conversationService = conversationService;
        this.aiProviderHealthService = aiProviderHealthService;
        this.sseTaskExecutor = sseTaskExecutor;
    }

    @GetMapping("/api/v1/assistants")
    @Operation(summary = "List AI assistants")
    public AssistantsResponse listAssistants() {
        return new AssistantsResponse(assistantRegistry.all().stream()
                .map(a -> new AssistantsResponse.AssistantDto(
                        a.role().name(),
                        a.pluginId(),
                        a.name(),
                        a.capabilities(),
                        a.limitations(),
                        a.toolIds()
                ))
                .toList());
    }

    @GetMapping("/api/v1/assistants/provider-health")
    @Operation(summary = "AI provider circuit state and optional live probes")
    public AiProviderHealthResponse providerHealth(@RequestParam(defaultValue = "false") boolean probe) {
        return new AiProviderHealthResponse(aiProviderHealthService.check(probe).stream()
                .map(h -> new AiProviderHealthResponse.ProviderHealthDto(
                        h.id(),
                        h.configured(),
                        h.circuitState(),
                        h.failureCount(),
                        h.circuitOpenUntil(),
                        h.probeStatus() == null ? null : (h.probeStatus() ? "up" : "down"),
                        h.probedAt()))
                .toList());
    }

    @GetMapping("/api/v1/projects/{projectId}/conversations")
    @Operation(summary = "List conversation threads for a project (optional assistantRole and q search)")
    public List<ConversationSummaryResponse> listConversations(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String assistantRole,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.listConversations(projectId, assistantRole, q, user.getId());
    }

    @GetMapping("/api/v1/projects/{projectId}/conversations/export")
    @Operation(summary = "Export all visible conversation threads for a project as JSON or Markdown archive")
    public ResponseEntity<byte[]> exportProjectConversations(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String assistantRole,
            @RequestParam(defaultValue = "markdown") String format,
            @RequestParam(required = false) String redaction,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ExportedConversation exported = conversationService.exportProjectConversations(
                projectId, assistantRole, format, user.getId(), redaction);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exported.filename() + "\"")
                .contentType(MediaType.parseMediaType(exported.contentType()))
                .body(exported.body());
    }

    @PostMapping("/api/v1/projects/{projectId}/conversations/retention-purge")
    @Operation(summary = "Delete expired conversation threads (skips legal hold); optional compliance export archive")
    public ResponseEntity<?> purgeExpiredConversations(
            @PathVariable UUID projectId,
            @RequestBody(required = false) RetentionPurgeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        boolean complianceExport = request != null && request.complianceExportRequested();
        ConversationService.RetentionPurgeResult result = conversationService.purgeExpiredConversations(
                projectId, user.getId(), complianceExport);
        if (complianceExport && result.complianceArchiveBody() != null) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + result.complianceArchiveFilename() + "\"")
                    .header("X-Purged-Count", String.valueOf(result.purgedCount()))
                    .header("X-Exported-Count", String.valueOf(result.exportedCount()))
                    .contentType(MediaType.parseMediaType("application/gzip"))
                    .body(result.complianceArchiveBody());
        }
        return ResponseEntity.ok(new RetentionPurgeResponse(result.purgedCount(), result.exportedCount()));
    }

    @PostMapping("/api/v1/projects/{projectId}/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new conversation thread")
    public ConversationSummaryResponse createConversation(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.createConversation(projectId, user.getId(), request);
    }

    @GetMapping("/api/v1/conversations/{conversationId}")
    @Operation(summary = "Get a conversation thread with messages")
    public ConversationResponse getConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.getConversation(conversationId, user.getId());
    }

    @PatchMapping("/api/v1/conversations/{conversationId}")
    @Operation(summary = "Rename a conversation thread")
    public ConversationSummaryResponse updateConversation(
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.updateConversation(conversationId, user.getId(), request);
    }

    @DeleteMapping("/api/v1/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a conversation thread")
    public void deleteConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        conversationService.deleteConversation(conversationId, user.getId());
    }

    @PostMapping("/api/v1/conversations/{conversationId}/share")
    @Operation(summary = "Enable read-only share link for a conversation thread")
    public ConversationShareResponse enableShare(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.enableShare(conversationId, user.getId());
    }

    @DeleteMapping("/api/v1/conversations/{conversationId}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke read-only share link for a conversation thread")
    public void revokeShare(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        conversationService.revokeShare(conversationId, user.getId());
    }

    @GetMapping("/api/v1/conversations/{conversationId}/export")
    @Operation(summary = "Export conversation thread as JSON or Markdown file")
    public ResponseEntity<byte[]> exportConversation(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "markdown") String format,
            @RequestParam(required = false) String redaction,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ExportedConversation exported = conversationService.exportConversation(
                conversationId, user.getId(), format, redaction);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exported.filename() + "\"")
                .contentType(MediaType.parseMediaType(exported.contentType()))
                .body(exported.body());
    }

    @PostMapping("/api/v1/conversations/{conversationId}/messages")
    @Operation(summary = "Send a chat message (non-streaming)")
    public ChatMessageResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.sendMessage(conversationId, user.getId(), request.content());
    }

    @PostMapping(
            value = "/api/v1/conversations/{conversationId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(summary = "Stream a chat message response (SSE)")
    public SseEmitter streamMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);
        UUID userId = user.getId();
        String content = request.content();
        Runnable task = () -> conversationService.streamMessage(conversationId, userId, content, emitter);
        sseTaskExecutor.execute(new DelegatingSecurityContextRunnable(task));
        return emitter;
    }
}
