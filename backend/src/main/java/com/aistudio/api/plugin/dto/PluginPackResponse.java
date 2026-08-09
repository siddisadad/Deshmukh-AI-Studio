package com.aistudio.api.plugin.dto;

import java.util.List;

public record PluginPackResponse(
        String id,
        String slug,
        String name,
        String publisher,
        String version,
        String description,
        boolean verified,
        List<String> pluginIds
) {
}
