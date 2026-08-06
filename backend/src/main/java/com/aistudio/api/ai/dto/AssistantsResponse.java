package com.aistudio.api.ai.dto;

import java.util.List;

public record AssistantsResponse(List<AssistantDto> assistants) {
    public record AssistantDto(
            String role,
            String pluginId,
            String name,
            List<String> capabilities,
            List<String> limitations,
            List<String> tools
    ) {
    }
}
