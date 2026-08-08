package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.sso")
public record SsoProperties(
        boolean enabled,
        String provider,
        String appBaseUrl,
        Oidc oidc,
        Saml saml
) {
    public record Oidc(
            String issuerUri,
            String clientId,
            String clientSecret,
            String displayName,
            String scopes
    ) {
        public boolean configured() {
            return issuerUri != null
                    && !issuerUri.isBlank()
                    && clientId != null
                    && !clientId.isBlank()
                    && clientSecret != null
                    && !clientSecret.isBlank();
        }

        public String resolvedScopes() {
            if (scopes == null || scopes.isBlank()) {
                return "openid email profile";
            }
            return scopes.trim();
        }

        public String resolvedDisplayName() {
            if (displayName == null || displayName.isBlank()) {
                return "Continue with SSO";
            }
            return displayName.trim();
        }
    }

    public record Saml(
            String metadataUrl,
            String entityId,
            String acsUrl,
            String displayName,
            boolean stubMode
    ) {
        public boolean configured() {
            return metadataUrl != null
                    && !metadataUrl.isBlank()
                    && entityId != null
                    && !entityId.isBlank()
                    && acsUrl != null
                    && !acsUrl.isBlank();
        }

        public String resolvedDisplayName() {
            if (displayName == null || displayName.isBlank()) {
                return "Continue with SAML";
            }
            return displayName.trim();
        }
    }
}
