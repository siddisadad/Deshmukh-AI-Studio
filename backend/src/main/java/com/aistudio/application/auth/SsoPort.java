package com.aistudio.application.auth;

import java.util.List;

/**
 * Identity provider port (OIDC authorization-code flow; SAML stub for dev/CI).
 * Mock for local/CI; OIDC for production IdPs; SAML stub until full SP binding ships.
 */
public interface SsoPort {

    String providerId();

    boolean enabled();

    String displayName();

    AuthorizationStart startAuthorization(String redirectUri, String state, String loginHint);

    UserInfo exchangeCode(String code, String redirectUri, String state);

    record AuthorizationStart(String authorizationUrl, String state) {
    }

    record UserInfo(
            String subject,
            String email,
            String displayName,
            boolean emailVerified
    ) {
    }

    record ProviderInfo(String id, String displayName) {
    }

    static List<ProviderInfo> toProviderInfos(List<SsoPort> ports) {
        return ports.stream()
                .filter(SsoPort::enabled)
                .map(p -> new ProviderInfo(p.providerId(), p.displayName()))
                .toList();
    }
}
