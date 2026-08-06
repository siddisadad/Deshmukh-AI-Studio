package com.aistudio.api.plugin.dto;

public record PluginResponse(
        String id,
        String name,
        String version,
        String type,
        String description,
        boolean builtin
) {
}
