package com.aistudio.api.ai.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @Size(max = 200) String title,
        @Pattern(regexp = "PROJECT|PRIVATE", message = "visibility must be PROJECT or PRIVATE") String visibility
) {
}
