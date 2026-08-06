package com.aistudio.api.plugin.dto;

import java.util.Map;

public record InvokeToolRequest(
        Map<String, Object> arguments
) {
}
