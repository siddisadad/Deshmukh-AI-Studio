package com.aistudio.api.ai.dto;

import java.time.Instant;

public record ConversationShareResponse(
        boolean shareEnabled,
        String shareUrl,
        String token,
        Instant expiresAt
) {
}
