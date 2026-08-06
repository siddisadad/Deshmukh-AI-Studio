package com.aistudio.api.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID projectId,
        String assistantRole,
        String title,
        List<MessageDto> messages
) {
    public record MessageDto(
            UUID id,
            String sender,
            String content,
            Instant createdAt
    ) {
    }
}
