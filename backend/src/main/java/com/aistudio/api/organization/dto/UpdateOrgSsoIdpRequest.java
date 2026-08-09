package com.aistudio.api.organization.dto;

public record UpdateOrgSsoIdpRequest(
        String displayName,
        Boolean enabled,
        String issuerUri,
        String clientId,
        String clientSecret,
        String scopes,
        String metadataUrl,
        String entityId,
        String acsUrl,
        String spPrivateKey,
        String spCertificate,
        Boolean wantEncryptedAssertions
) {
}
