package com.aistudio.api.ai;

import com.aistudio.api.ai.dto.AssistantsResponse;
import com.aistudio.api.ai.dto.ChatMessageResponse;
import com.aistudio.api.ai.dto.ConversationResponse;
import com.aistudio.api.ai.dto.SendMessageRequest;
import com.aistudio.application.ai.AssistantRegistry;
import com.aistudio.application.ai.ConversationService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Assistants")
public class AssistantController {

    private final AssistantRegistry assistantRegistry;
    private final ConversationService conversationService;
    private final Executor sseTaskExecutor;

    public AssistantController(
            AssistantRegistry assistantRegistry,
            ConversationService conversationService,
            @Qualifier("sseTaskExecutor") Executor sseTaskExecutor
    ) {
        this.assistantRegistry = assistantRegistry;
        this.conversationService = conversationService;
        this.sseTaskExecutor = sseTaskExecutor;
    }

    @GetMapping("/api/v1/assistants")
    @Operation(summary = "List AI assistants")
    public AssistantsResponse listAssistants() {
        return new AssistantsResponse(assistantRegistry.all().stream()
                .map(a -> new AssistantsResponse.AssistantDto(
                        a.role().name(),
                        a.name(),
                        a.capabilities(),
                        a.limitations()
                ))
                .toList());
    }

    @GetMapping("/api/v1/projects/{projectId}/conversations/{assistantRole}")
    @Operation(summary = "Get conversation history for an assistant")
    public ConversationResponse getConversation(
            @PathVariable UUID projectId,
            @PathVariable String assistantRole,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.getConversation(projectId, assistantRole, user.getId());
    }

    @PostMapping("/api/v1/projects/{projectId}/conversations/{assistantRole}/messages")
    @Operation(summary = "Send a chat message to an assistant")
    public ChatMessageResponse sendMessage(
            @PathVariable UUID projectId,
            @PathVariable String assistantRole,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return conversationService.sendMessage(projectId, assistantRole, user.getId(), request.content());
    }

    @PostMapping(
            value = "/api/v1/projects/{projectId}/conversations/{assistantRole}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(summary = "Stream a chat message response (SSE)")
    public SseEmitter streamMessage(
            @PathVariable UUID projectId,
            @PathVariable String assistantRole,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SseEmitter emitter = new SseEmitter(120_000L);
        UUID userId = user.getId();
        String content = request.content();
        Runnable task = () -> conversationService.streamMessage(projectId, assistantRole, userId, content, emitter);
        sseTaskExecutor.execute(new DelegatingSecurityContextRunnable(task));
        return emitter;
    }
}
