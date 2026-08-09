package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.auth.OrgSsoIdpProtocol;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class OidcConfiguredSsoPort implements SsoPort {

    private final OrgSsoIdpEntity idp;
    private final OrgSsoIdpRepository idpRepository;
    private final OidcDiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final SsoPendingAuthStore pendingStore;
    private final SsoProperties ssoProperties;

    public OidcConfiguredSsoPort(
            OrgSsoIdpEntity idp,
            OrgSsoIdpRepository idpRepository,
            OidcDiscoveryClient discoveryClient,
            RestClient restClient,
            SsoPendingAuthStore pendingStore,
            SsoProperties ssoProperties
    ) {
        this.idp = idp;
        this.idpRepository = idpRepository;
        this.discoveryClient = discoveryClient;
        this.restClient = restClient;
        this.pendingStore = pendingStore;
        this.ssoProperties = ssoProperties;
    }

    @Override
    public String providerId() {
        return idp.providerId();
    }

    @Override
    public boolean enabled() {
        return idp.isEnabled() && ssoProperties.enabled();
    }

    @Override
    public String displayName() {
        return idp.getDisplayName();
    }

    @Override
    public AuthorizationStart startAuthorization(String redirectUri, String state, String loginHint) {
        OrgSsoIdpEntity current = requireCurrent();
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "redirectUri is required");
        }
        OidcDiscoveryDocument discovery = discoveryClient.discoverByIssuer(current.getIssuerUri());
        pendingStore.putOidcStart(state, current.getId(), redirectUri.trim(), System.currentTimeMillis() + 600_000L);

        String scopes = current.getScopes() == null || current.getScopes().isBlank()
                ? "openid email profile"
                : current.getScopes().trim();
        StringBuilder url = new StringBuilder(discovery.authorizationEndpoint())
                .append("?response_type=code")
                .append("&client_id=").append(enc(current.getClientId()))
                .append("&redirect_uri=").append(enc(redirectUri))
                .append("&scope=").append(enc(scopes))
                .append("&state=").append(enc(state));
        if (loginHint != null && !loginHint.isBlank()) {
            url.append("&login_hint=").append(enc(loginHint.trim()));
        }
        return new AuthorizationStart(url.toString(), state);
    }

    @Override
    public UserInfo exchangeCode(String code, String redirectUri, String state) {
        OrgSsoIdpEntity current = requireCurrent();
        SsoPendingAuthStore.PendingOidcStart pending = pendingStore.removeOidcStart(state)
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", "SSO state is invalid or expired"));
        if (pending.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SSO state is invalid or expired");
        }
        if (!pending.idpId().equals(current.getId())) {
            throw new DomainException("INVALID_TOKEN", "SSO state does not match provider");
        }
        if (code == null || code.isBlank()) {
            throw new DomainException("INVALID_TOKEN", "SSO authorization code is required");
        }
        String resolvedRedirect = redirectUri == null || redirectUri.isBlank() ? pending.redirectUri() : redirectUri;
        if (!resolvedRedirect.equals(pending.redirectUri())) {
            throw new DomainException("INVALID_TOKEN", "SSO redirect URI mismatch");
        }

        OidcDiscoveryDocument discovery = discoveryClient.discoverByIssuer(current.getIssuerUri());
        OidcTokenResponse token = restClient.post()
                .uri(discovery.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=authorization_code"
                        + "&code=" + enc(code)
                        + "&redirect_uri=" + enc(resolvedRedirect)
                        + "&client_id=" + enc(current.getClientId())
                        + "&client_secret=" + enc(current.getClientSecret()))
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
                userInfo.email().trim().toLowerCase(Locale.ROOT),
                displayName,
                userInfo.emailVerified() != null && userInfo.emailVerified()
        );
    }

    private OrgSsoIdpEntity requireCurrent() {
        return idpRepository.findById(idp.getId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO IdP configuration not found"));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
