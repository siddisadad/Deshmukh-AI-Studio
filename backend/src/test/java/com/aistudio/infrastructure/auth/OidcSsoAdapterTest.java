package com.aistudio.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.infrastructure.config.SsoProperties;
import org.junit.jupiter.api.Test;

class OidcSsoAdapterTest {

    @Test
    void startAuthorizationBuildsAuthorizeUrl() {
        OidcDiscoveryDocument discovery = new OidcDiscoveryDocument(
                "https://issuer.example.com",
                "https://issuer.example.com/authorize",
                "https://issuer.example.com/token",
                "https://issuer.example.com/userinfo"
        );
        OidcDiscoveryClient discoveryClient = new OidcDiscoveryClient(org.springframework.web.client.RestClient.builder()) {
            @Override
            public OidcDiscoveryDocument discover(SsoProperties.Oidc oidc) {
                return discovery;
            }
        };
        SsoProperties properties = new SsoProperties(
                true,
                "oidc",
                "http://localhost:5173",
                new SsoProperties.Oidc(
                        "https://issuer.example.com",
                        "client-id",
                        "client-secret",
                        "Continue with Okta",
                        "openid email profile"
                )
        );
        OidcSsoAdapter adapter = new OidcSsoAdapter(
                properties,
                discoveryClient,
                org.springframework.web.client.RestClient.builder()
        );

        SsoPort.AuthorizationStart start = adapter.startAuthorization(
                "http://localhost:5173/auth/sso/callback",
                "state123",
                "user@example.com"
        );

        assertEquals("state123", start.state());
        assertTrue(start.authorizationUrl().startsWith("https://issuer.example.com/authorize"));
        assertTrue(start.authorizationUrl().contains("client_id=client-id"));
        assertTrue(start.authorizationUrl().contains("state=state123"));
        assertTrue(start.authorizationUrl().contains("login_hint=user%40example.com"));
    }
}
