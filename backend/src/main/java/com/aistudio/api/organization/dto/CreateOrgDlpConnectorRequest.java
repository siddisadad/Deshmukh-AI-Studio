package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateOrgDlpConnectorRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,62}[a-z0-9]") String slug,
        @NotBlank String connectorType,
        @NotBlank String displayName,
        @NotBlank String webhookUrl,
        boolean enabled,
        boolean blockOnMatch,
        String customPatternsJson
) {
}
