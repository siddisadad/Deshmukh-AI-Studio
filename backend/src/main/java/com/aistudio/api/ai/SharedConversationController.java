package com.aistudio.api.ai;

import com.aistudio.api.ai.dto.SharedConversationResponse;
import com.aistudio.application.ai.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Shared conversations")
public class SharedConversationController {

    private final ConversationService conversationService;

    public SharedConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/api/v1/shared/conversations/{token}")
    @Operation(summary = "Read-only shared conversation (public link)")
    public SharedConversationResponse getSharedConversation(@PathVariable String token) {
        return conversationService.getSharedConversation(token);
    }
}
