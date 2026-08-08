package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * SAML SSO stub for dev/CI. Uses redirect-style callback parameters (like OIDC mock).
 */
@Component
@ConditionalOnExpression("'${aistudio.sso.provider:mock}' == 'saml' && '${aistudio.sso.saml.stub-mode:true}' == 'true'")
public class SamlSsoStubAdapter implements SsoPort {

    private final SsoProperties ssoProperties;
    private final String appBaseUrl;
    private final Map<String, InstantState> pendingStates = new ConcurrentHashMap<>();

    public SamlSsoStubAdapter(SsoProperties ssoProperties) {
        this.ssoProperties = ssoProperties;
        String base = ssoProperties.appBaseUrl();
        this.appBaseUrl = base == null || base.isBlank()
                ? "http://localhost:5173"
                : (base.endsWith("/") ? base.substring(0, base.length() - 1) : base);
    }

    @Override
    public String providerId() {
        return "saml";
    }

    @Override
    public boolean enabled() {
        return ssoProperties.enabled();
    }

    @Override
    public String displayName() {
        return ssoProperties.saml().resolvedDisplayName();
    }

    @Override
    public AuthorizationStart startAuthorization(String redirectUri, String state, String loginHint) {
        String target = (redirectUri == null || redirectUri.isBlank())
                ? appBaseUrl + "/auth/sso/callback"
                : redirectUri;
        pendingStates.put(state, new InstantState(System.currentTimeMillis() + 600_000L));
        String email;
        String displayName;
        if (loginHint != null && loginHint.contains("@")) {
            email = loginHint.trim().toLowerCase(Locale.ROOT);
            displayName = email.substring(0, email.indexOf('@'));
        } else {
            email = "saml.user+" + state.substring(0, Math.min(8, state.length())) + "@example.com";
            displayName = "SAML User";
        }
        String code = "saml." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString((email + "|" + displayName).getBytes(StandardCharsets.UTF_8));
        String url = target
                + (target.contains("?") ? "&" : "?")
                + "provider=saml"
                + "&state=" + encode(state)
                + "&code=" + encode(code);
        return new AuthorizationStart(url, state);
    }

    @Override
    public UserInfo exchangeCode(String code, String redirectUri, String state) {
        InstantState pending = pendingStates.remove(state);
        if (pending == null || pending.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SSO state is invalid or expired");
        }
        if (code == null || !code.startsWith("saml.")) {
            throw new DomainException("INVALID_TOKEN", "Invalid SAML authorization code");
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(code.substring("saml.".length())),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", 2);
            String email = parts[0].trim().toLowerCase(Locale.ROOT);
            String displayName = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : "SAML User";
            return new UserInfo("saml|" + email, email, displayName, true);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("INVALID_TOKEN", "Invalid SAML authorization code");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record InstantState(long expiresAtMs) {
    }
}
