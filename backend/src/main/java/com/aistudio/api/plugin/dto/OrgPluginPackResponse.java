package com.aistudio.api.plugin.dto;

import java.time.Instant;

public record OrgPluginPackResponse(
        PluginPackResponse pack,
        boolean installed,
        Instant installedAt
) {
}
