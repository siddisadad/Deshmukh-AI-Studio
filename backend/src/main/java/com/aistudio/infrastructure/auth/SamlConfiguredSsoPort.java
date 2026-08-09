package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.settings.Saml2Settings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SamlConfiguredSsoPort implements SsoPort {

    private static final String OTC_PREFIX = "saml.otc.";

    private final OrgSsoIdpEntity idp;
    private final OrgSsoIdpRepository idpRepository;
    private final SsoPendingAuthStore pendingStore;
    private final SsoProperties ssoProperties;
    private final ObjectMapper objectMapper;

    public SamlConfiguredSsoPort(
            OrgSsoIdpEntity idp,
            OrgSsoIdpRepository idpRepository,
            SsoPendingAuthStore pendingStore,
            SsoProperties ssoProperties,
            ObjectMapper objectMapper
    ) {
        this.idp = idp;
        this.idpRepository = idpRepository;
        this.pendingStore = pendingStore;
        this.ssoProperties = ssoProperties;
        this.objectMapper = objectMapper;
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
        try {
            Saml2Settings settings = SamlConfiguredSettingsBuilder.build(current, objectMapper);
            SamlAuthnRedirectBuilder.Redirect redirect = SamlAuthnRedirectBuilder.build(settings, state);
            pendingStore.putSamlStart(
                    state,
                    current.getId(),
                    redirectUri.trim(),
                    redirect.requestId(),
                    System.currentTimeMillis() + 600_000L
            );
            return new AuthorizationStart(redirect.authorizationUrl(), state);
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Failed to start SAML authorization");
        }
    }

    public String completeAcs(String samlResponse, String relayState) {
        SsoPendingAuthStore.PendingSamlStart pending = pendingStore.removeSamlStart(relayState)
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", "SSO state is invalid or expired"));
        if (pending.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SSO state is invalid or expired");
        }
        OrgSsoIdpEntity current = idpRepository.findById(pending.idpId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO IdP configuration not found"));
        if (samlResponse == null || samlResponse.isBlank()) {
            throw new DomainException("INVALID_TOKEN", "SAMLResponse is required");
        }
        try {
            Saml2Settings settings = SamlConfiguredSettingsBuilder.build(current, objectMapper);
            HttpRequest request = new HttpRequest(current.getAcsUrl().trim());
            request.addParameter("SAMLResponse", samlResponse);
            request.addParameter("RelayState", relayState);
            SamlResponse response = new SamlResponse(settings, request);
            if (!response.isValid(pending.requestId())) {
                throw new DomainException("INVALID_TOKEN", "Invalid SAML response");
            }
            UserInfo userInfo = toUserInfo(response);
            String exchangeCode = OTC_PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString((userInfo.email() + "|" + userInfo.displayName()).getBytes(StandardCharsets.UTF_8));
            pendingStore.putSamlExchange(exchangeCode, userInfo, System.currentTimeMillis() + 120_000L);
            return pending.redirectUri()
                    + (pending.redirectUri().contains("?") ? "&" : "?")
                    + "provider=" + encode(current.providerId())
                    + "&state=" + encode(relayState)
                    + "&code=" + encode(exchangeCode);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("INVALID_TOKEN", "Invalid SAML response");
        }
    }

    @Override
    public UserInfo exchangeCode(String code, String redirectUri, String state) {
        if (code == null || !code.startsWith(OTC_PREFIX)) {
            throw new DomainException("INVALID_TOKEN", "Invalid SAML authorization code");
        }
        SsoPendingAuthStore.PendingSamlExchange exchange = pendingStore.removeSamlExchange(code)
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", "SAML authorization code is invalid or expired"));
        if (exchange.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SAML authorization code is invalid or expired");
        }
        return exchange.userInfo();
    }

    private OrgSsoIdpEntity requireCurrent() {
        return idpRepository.findById(idp.getId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO IdP configuration not found"));
    }

    private static UserInfo toUserInfo(SamlResponse response) throws Exception {
        String email = response.getNameId();
        if (email == null || email.isBlank() || !email.contains("@")) {
            email = firstAttribute(response, "email", "mail", "Email",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException("INVALID_TOKEN", "SAML assertion missing email");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        String displayName = firstAttribute(response, "displayName", "name", "Name",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name");
        if (displayName == null || displayName.isBlank()) {
            displayName = email.substring(0, email.indexOf('@'));
        }
        String subject = response.getNameId();
        if (subject == null || subject.isBlank()) {
            subject = "saml|" + email;
        }
        return new UserInfo(subject.trim(), email, displayName.trim(), true);
    }

    private static String firstAttribute(SamlResponse response, String... names) throws Exception {
        HashMap<String, List<String>> attributes = response.getAttributes();
        if (attributes == null) {
            return null;
        }
        for (String name : names) {
            List<String> values = attributes.get(name);
            if (values != null && !values.isEmpty() && values.get(0) != null && !values.get(0).isBlank()) {
                return values.get(0);
            }
        }
        return null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
