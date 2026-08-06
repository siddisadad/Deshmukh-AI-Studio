package com.aistudio.api.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        MessageDto userMessage,
        MessageDto assistantMessage,
        String provider,
        String model
) {
    public record MessageDto(
            UUID id,
            String sender,
            String content,
            Instant createdAt
    ) {
    }
}
