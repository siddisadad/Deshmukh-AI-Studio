package com.aistudio.infrastructure.auth;

import com.onelogin.saml2.authn.AuthnRequest;
import com.onelogin.saml2.authn.AuthnRequestParams;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.util.Util;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

final class SamlAuthnRedirectBuilder {

    record Redirect(String authorizationUrl, String requestId) {
    }

    private SamlAuthnRedirectBuilder() {
    }

    static Redirect build(Saml2Settings settings, String relayState) {
        try {
            AuthnRequest authnRequest = new AuthnRequest(
                    settings,
                    new AuthnRequestParams(false, false, true));
            String samlRequest = authnRequest.getEncodedAuthnRequest(true);
            List<String> queryParts = new ArrayList<>();
            queryParts.add("SAMLRequest=" + encode(samlRequest));
            if (relayState != null && !relayState.isBlank()) {
                queryParts.add("RelayState=" + encode(relayState));
            }
            if (settings.getAuthnRequestsSigned()) {
                String sigAlg = settings.getSignatureAlgorithm();
                String signature = signRedirect(settings, samlRequest, relayState, sigAlg);
                queryParts.add("SigAlg=" + encode(sigAlg));
                queryParts.add("Signature=" + encode(signature));
            }
            String idpUrl = settings.getIdpSingleSignOnServiceUrl().toString();
            String separator = idpUrl.contains("?") ? "&" : "?";
            String authorizationUrl = idpUrl + separator + String.join("&", queryParts);
            return new Redirect(authorizationUrl, authnRequest.getId());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build SAML AuthnRequest", ex);
        }
    }

    private static String signRedirect(
            Saml2Settings settings,
            String samlRequest,
            String relayState,
            String sigAlg
    ) {
        if (!settings.checkSPCerts()) {
            throw new IllegalStateException("SP private key is required for signed AuthnRequest");
        }
        PrivateKey key = settings.getSPkey();
        String message = "SAMLRequest=" + Util.urlEncoder(samlRequest);
        if (relayState != null && !relayState.isBlank()) {
            message += "&RelayState=" + Util.urlEncoder(relayState);
        }
        message += "&SigAlg=" + Util.urlEncoder(sigAlg);
        try {
            return Util.base64encoder(Util.sign(message, key, sigAlg));
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
            throw new IllegalStateException("Failed to sign SAML AuthnRequest", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
