package com.aistudio.api.plugin.dto;

public record OrgPluginResponse(
        PluginResponse plugin,
        boolean enabled,
        boolean canDisable
) {
}
