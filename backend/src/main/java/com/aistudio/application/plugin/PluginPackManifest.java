package com.aistudio.application.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginPackManifest(
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
