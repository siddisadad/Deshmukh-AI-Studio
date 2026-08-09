package com.aistudio.api.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateOrgSsoIdpRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,62}[a-z0-9]") String slug,
        @NotBlank String protocol,
        @NotBlank String displayName,
        boolean enabled,
        String issuerUri,
        String clientId,
        String clientSecret,
        String scopes,
        String metadataUrl,
        String entityId,
        String acsUrl,
        String spPrivateKey,
        String spCertificate,
        boolean wantEncryptedAssertions
) {
}
