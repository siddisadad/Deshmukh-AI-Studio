package com.aistudio.api.plugin.dto;

import java.util.Map;

public record InvokeToolResponse(
        String toolId,
        String toolName,
        boolean success,
        String output,
        Map<String, Object> metadata
) {
}
