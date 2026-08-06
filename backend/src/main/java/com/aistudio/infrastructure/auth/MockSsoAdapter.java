package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.common.DomainException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dev/CI SSO adapter. Authorization URL lands on the SPA callback with a mock code.
 * Code format: {@code mock.<base64url(email|displayName)>}
 */
@Component
@ConditionalOnProperty(name = "aistudio.sso.provider", havingValue = "mock", matchIfMissing = true)
public class MockSsoAdapter implements SsoPort {

    private final boolean enabled;
    private final String appBaseUrl;
    private final Map<String, InstantState> pendingStates = new ConcurrentHashMap<>();

    public MockSsoAdapter(
            @Value("${aistudio.sso.enabled:true}") boolean enabled,
            @Value("${aistudio.sso.app-base-url:${aistudio.billing.app-base-url:http://localhost:5173}}") String appBaseUrl
    ) {
        this.enabled = enabled;
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    @Override
    public String providerId() {
        return "mock";
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public String displayName() {
        return "Continue with SSO (Mock)";
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
            email = "sso.user+" + state.substring(0, Math.min(8, state.length())) + "@example.com";
            displayName = "SSO User";
        }
        String code = "mock." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString((email + "|" + displayName).getBytes(StandardCharsets.UTF_8));
        String url = target
                + (target.contains("?") ? "&" : "?")
                + "provider=mock"
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
        if (code == null || !code.startsWith("mock.")) {
            throw new DomainException("INVALID_TOKEN", "Invalid SSO authorization code");
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(code.substring("mock.".length())),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", 2);
            String email = parts[0].trim().toLowerCase(Locale.ROOT);
            String displayName = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : "SSO User";
            return new UserInfo("mock|" + email, email, displayName, true);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("INVALID_TOKEN", "Invalid SSO authorization code");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record InstantState(long expiresAtMs) {
    }
}
