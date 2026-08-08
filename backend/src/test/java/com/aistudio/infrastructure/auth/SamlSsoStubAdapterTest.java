package com.aistudio.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.infrastructure.config.SsoProperties;
import org.junit.jupiter.api.Test;

class SamlSsoStubAdapterTest {

    @Test
    void stubModeStartAndExchangeCode() {
        SamlSsoStubAdapter adapter = new SamlSsoStubAdapter(stubProperties(true));

        SsoPort.AuthorizationStart start = adapter.startAuthorization(
                "http://localhost:5173/auth/sso/callback",
                "state-saml",
                "saml.user@example.com"
        );

        assertEquals("state-saml", start.state());
        assertTrue(start.authorizationUrl().contains("provider=saml"));
        assertTrue(start.authorizationUrl().contains("state=state-saml"));
        assertTrue(start.authorizationUrl().contains("code=saml."));

        String code = extractCode(start.authorizationUrl());
        SsoPort.UserInfo user = adapter.exchangeCode(
                code,
                "http://localhost:5173/auth/sso/callback",
                "state-saml"
        );
        assertEquals("saml.user@example.com", user.email());
        assertEquals("saml|saml.user@example.com", user.subject());
        assertTrue(user.emailVerified());
    }

    private static SsoProperties stubProperties(boolean stubMode) {
        return new SsoProperties(
                true,
                "saml",
                "http://localhost:5173",
                new SsoProperties.Oidc("", "", "", "", ""),
                new SsoProperties.Saml(
                        stubMode ? "" : "https://idp.example.com/metadata",
                        stubMode ? "" : "https://app.example.com/saml",
                        stubMode ? "" : "https://api.example.com/api/v1/auth/sso/saml/acs",
                        "Continue with SAML (stub)",
                        stubMode
                )
        );
    }

    private static String extractCode(String authorizationUrl) {
        int idx = authorizationUrl.indexOf("code=");
        if (idx < 0) {
            throw new IllegalArgumentException("missing code param: " + authorizationUrl);
        }
        String rest = authorizationUrl.substring(idx + 5);
        int amp = rest.indexOf('&');
        return amp < 0 ? rest : rest.substring(0, amp);
    }
}
