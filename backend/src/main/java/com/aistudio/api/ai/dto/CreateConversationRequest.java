package com.aistudio.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank String assistantRole,
        @Size(max = 200) String title
) {
}
