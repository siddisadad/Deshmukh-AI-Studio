package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "aistudio.sso.provider", havingValue = "oidc")
public class OidcSsoAdapter implements SsoPort {

    private final SsoProperties ssoProperties;
    private final OidcDiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final Map<String, PendingState> pendingStates = new ConcurrentHashMap<>();

    public OidcSsoAdapter(
            SsoProperties ssoProperties,
            OidcDiscoveryClient discoveryClient,
            RestClient.Builder restClientBuilder
    ) {
        this.ssoProperties = ssoProperties;
        this.discoveryClient = discoveryClient;
        if (!ssoProperties.oidc().configured()) {
            throw new IllegalStateException(
                    "aistudio.sso.provider=oidc requires OIDC_ISSUER_URI, OIDC_CLIENT_ID, and OIDC_CLIENT_SECRET"
            );
        }
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String providerId() {
        return "oidc";
    }

    @Override
    public boolean enabled() {
        return ssoProperties.enabled();
    }

    @Override
    public String displayName() {
        return ssoProperties.oidc().resolvedDisplayName();
    }

    @Override
    public AuthorizationStart startAuthorization(String redirectUri, String state, String loginHint) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "redirectUri is required");
        }
        OidcDiscoveryDocument discovery = discoveryClient.discover(ssoProperties.oidc());
        pendingStates.put(state, new PendingState(redirectUri, System.currentTimeMillis() + 600_000L));

        StringBuilder url = new StringBuilder(discovery.authorizationEndpoint())
                .append("?response_type=code")
                .append("&client_id=").append(enc(ssoProperties.oidc().clientId()))
                .append("&redirect_uri=").append(enc(redirectUri))
                .append("&scope=").append(enc(ssoProperties.oidc().resolvedScopes()))
                .append("&state=").append(enc(state));
        if (loginHint != null && !loginHint.isBlank()) {
            url.append("&login_hint=").append(enc(loginHint.trim()));
        }
        return new AuthorizationStart(url.toString(), state);
    }

    @Override
    public UserInfo exchangeCode(String code, String redirectUri, String state) {
        PendingState pending = pendingStates.remove(state);
        if (pending == null || pending.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SSO state is invalid or expired");
        }
        if (code == null || code.isBlank()) {
            throw new DomainException("INVALID_TOKEN", "SSO authorization code is required");
        }
        String resolvedRedirect = redirectUri == null || redirectUri.isBlank() ? pending.redirectUri() : redirectUri;
        if (!resolvedRedirect.equals(pending.redirectUri())) {
            throw new DomainException("INVALID_TOKEN", "SSO redirect URI mismatch");
        }

        OidcDiscoveryDocument discovery = discoveryClient.discover(ssoProperties.oidc());
        OidcTokenResponse token = restClient.post()
                .uri(discovery.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=authorization_code"
                        + "&code=" + enc(code)
                        + "&redirect_uri=" + enc(resolvedRedirect)
                        + "&client_id=" + enc(ssoProperties.oidc().clientId())
                        + "&client_secret=" + enc(ssoProperties.oidc().clientSecret()))
                .retrieve()
                .body(OidcTokenResponse.class);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new DomainException("INVALID_TOKEN", "OIDC token exchange failed");
        }

        OidcUserInfoResponse userInfo = restClient.get()
                .uri(discovery.userinfoEndpoint())
                .header("Authorization", "Bearer " + token.accessToken())
                .retrieve()
                .body(OidcUserInfoResponse.class);
        if (userInfo == null || userInfo.subject() == null || userInfo.subject().isBlank()) {
            throw new DomainException("INVALID_TOKEN", "OIDC userinfo missing subject");
        }
        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new DomainException("INVALID_TOKEN", "OIDC userinfo missing email");
        }
        String displayName = userInfo.name() == null || userInfo.name().isBlank()
                ? userInfo.email().substring(0, userInfo.email().indexOf('@'))
                : userInfo.name().trim();
        return new UserInfo(
                userInfo.subject(),
                userInfo.email().trim().toLowerCase(),
                displayName,
                userInfo.emailVerified() != null && userInfo.emailVerified()
        );
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record PendingState(String redirectUri, long expiresAtMs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OidcTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OidcUserInfoResponse(
            @JsonProperty("sub") String subject,
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("email_verified") Boolean emailVerified
    ) {
    }
}
