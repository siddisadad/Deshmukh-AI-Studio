package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import com.onelogin.saml2.authn.AuthnRequest;
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
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * SAML SP-initiated login (HTTP-Redirect AuthnRequest + HTTP-POST SAMLResponse ACS).
 */
@Component
@ConditionalOnExpression("'${aistudio.sso.provider:mock}' == 'saml' && '${aistudio.sso.saml.stub-mode:true}' == 'false'")
public class SamlSpSsoAdapter implements SsoPort {

    private static final String OTC_PREFIX = "saml.otc.";

    private final SsoProperties ssoProperties;
    private final SamlSettingsService settingsService;
    private final Map<String, PendingStart> pendingStarts = new ConcurrentHashMap<>();
    private final Map<String, PendingExchange> pendingExchanges = new ConcurrentHashMap<>();

    public SamlSpSsoAdapter(SsoProperties ssoProperties, SamlSettingsService settingsService) {
        this.ssoProperties = ssoProperties;
        this.settingsService = settingsService;
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
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "redirectUri is required");
        }
        try {
            Saml2Settings settings = settingsService.settings();
            AuthnRequest authnRequest = new AuthnRequest(settings, false, false, true);
            String samlRequest = authnRequest.getEncodedAuthnRequest(true);
            String idpUrl = settings.getIdpSingleSignOnServiceUrl().toString();
            String authorizationUrl = idpUrl
                    + (idpUrl.contains("?") ? "&" : "?")
                    + "SAMLRequest=" + encode(samlRequest)
                    + "&RelayState=" + encode(state);
            pendingStarts.put(state, new PendingStart(
                    redirectUri.trim(),
                    authnRequest.getId(),
                    System.currentTimeMillis() + 600_000L
            ));
            return new AuthorizationStart(authorizationUrl, state);
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Failed to start SAML authorization");
        }
    }

    public String completeAcs(String samlResponse, String relayState) {
        PendingStart pending = pendingStarts.remove(relayState);
        if (pending == null || pending.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SSO state is invalid or expired");
        }
        if (samlResponse == null || samlResponse.isBlank()) {
            throw new DomainException("INVALID_TOKEN", "SAMLResponse is required");
        }
        try {
            HttpRequest request = new HttpRequest(ssoProperties.saml().acsUrl().trim());
            request.addParameter("SAMLResponse", samlResponse);
            request.addParameter("RelayState", relayState);
            SamlResponse response = new SamlResponse(settingsService.settings(), request);
            if (!response.isValid(pending.requestId())) {
                throw new DomainException("INVALID_TOKEN", "Invalid SAML response");
            }
            UserInfo userInfo = toUserInfo(response);
            String exchangeCode = OTC_PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString((userInfo.email() + "|" + userInfo.displayName()).getBytes(StandardCharsets.UTF_8));
            pendingExchanges.put(exchangeCode, new PendingExchange(
                    userInfo,
                    System.currentTimeMillis() + 120_000L
            ));
            return pending.redirectUri()
                    + (pending.redirectUri().contains("?") ? "&" : "?")
                    + "provider=saml"
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
        PendingExchange exchange = pendingExchanges.remove(code);
        if (exchange == null || exchange.expiresAtMs() < System.currentTimeMillis()) {
            throw new DomainException("INVALID_TOKEN", "SAML authorization code is invalid or expired");
        }
        return exchange.userInfo();
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

    private record PendingStart(String redirectUri, String requestId, long expiresAtMs) {
    }

    private record PendingExchange(UserInfo userInfo, long expiresAtMs) {
    }
}
