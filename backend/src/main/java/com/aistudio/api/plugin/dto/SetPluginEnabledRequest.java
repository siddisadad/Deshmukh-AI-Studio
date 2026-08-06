package com.aistudio.api.plugin.dto;

import jakarta.validation.constraints.NotNull;

public record SetPluginEnabledRequest(
        @NotNull Boolean enabled
) {
}
