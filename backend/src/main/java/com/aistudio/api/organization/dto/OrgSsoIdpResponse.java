package com.aistudio.api.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgSsoIdpResponse(
        UUID id,
        String slug,
        String protocol,
        String displayName,
        boolean enabled,
        String issuerUri,
        String clientId,
        boolean clientSecretConfigured,
        String scopes,
        String metadataUrl,
        String entityId,
        String acsUrl,
        boolean spSigningConfigured,
        boolean wantEncryptedAssertions,
        Instant metadataFetchedAt,
        String metadataRefreshError
) {
}
