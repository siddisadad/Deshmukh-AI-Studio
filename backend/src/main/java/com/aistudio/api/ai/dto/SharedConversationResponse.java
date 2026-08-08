package com.aistudio.api.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SharedConversationResponse(
        String assistantRole,
        String title,
        Instant expiresAt,
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
