package com.aistudio.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.onelogin.saml2.settings.SettingsBuilder;
import com.onelogin.saml2.settings.Saml2Settings;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SamlAuthnRedirectBuilderTest {

    @Test
    void signedRedirectIncludesSignatureParameters() throws Exception {
        String privateKey = readFixture("saml/sp-test-private.pem");
        String certificate = readFixture("saml/sp-test-certificate.pem");
        Map<String, Object> values = baseSettings(privateKey, certificate, true);
        Saml2Settings settings = new SettingsBuilder().fromValues(values).build();

        SamlAuthnRedirectBuilder.Redirect redirect = SamlAuthnRedirectBuilder.build(settings, "relay-state-1");

        assertNotNull(redirect.requestId());
        assertTrue(redirect.authorizationUrl().contains("Signature="));
        assertTrue(redirect.authorizationUrl().contains("SigAlg="));
        assertTrue(redirect.authorizationUrl().contains("SAMLRequest="));
        assertTrue(redirect.authorizationUrl().contains("RelayState="));
    }

    @Test
    void unsignedRedirectOmitsSignatureParameters() throws Exception {
        Map<String, Object> values = baseSettings(null, null, false);
        Saml2Settings settings = new SettingsBuilder().fromValues(values).build();

        SamlAuthnRedirectBuilder.Redirect redirect = SamlAuthnRedirectBuilder.build(settings, "relay-state-2");

        assertTrue(!redirect.authorizationUrl().contains("Signature="));
    }

    private static Map<String, Object> baseSettings(String privateKey, String certificate, boolean signed) {
        Map<String, Object> values = new HashMap<>();
        values.put("onelogin.saml2.sp.entityid", "https://sp.example.com/metadata");
        values.put("onelogin.saml2.sp.assertion_consumer_service.url", "https://sp.example.com/acs");
        values.put("onelogin.saml2.idp.entityid", "https://idp.example.com/metadata");
        values.put("onelogin.saml2.idp.single_sign_on_service.url", "https://idp.example.com/sso/redirect");
        values.put(
                "onelogin.saml2.idp.x509cert",
                "MIIC+zCCAeOgAwIBAgIJAL5Z2w8x8Y7TMA0GCSqGSIb3DQEBCwUAMIGLMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQ0ExFjAUBgNVBAcTDU1vdW50YWluIFZpZXcxFDASBgNVBAoTC0Rlc2htb2NvIENvLjERMA8GA1UECxMIQ3J5cHRvIFBybzEZMBcGA1UEAxMQd3d3LmRlc2htb2NvLmNvbTEhMB8GCSqGSIb3DQEBEJARYSc3NsQWRtaW5AZGVzaG1vY28uY29tMB4XDTA0MDExNzA3NDUzNVoXDTA0MDIxNjA3NDUzNVowgY8xCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDQTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHUHJpbWF0ZTELMAsGA1UECxMCU0ExGDAWBgNVBAMTD3ByaW1hdGVzYW1sc2lnbjETMBEGA1UEAxMKaWRwLmV4YW1wbGUuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDUoHY4LhpnRSdYfCvZBvQh1zEKAiSL6Z5nFq+rY6m2hO0PZAGI5ZqJ0zPr9uo9z3eqN8nNbv4feF3PnF7p4uT2AB0dAWg/oim3RdeD464eAIN08GQWKYjWeWKjYImWPM/G11Hu7w2l9af0hi1J4sRfHrHDA9WoXSazfX8mQwIDAQABMA0GCSqGSIb3DQEBBQUAA4GBAH4mpWHroBYjPt9Q0AIV9u5jUw6LRYaafH4VqWjnC27rIepXpouKKoVHemED7Eq2kfF+5ZL9u2EYul/W2auC2NxTKfUvGXZ5Y5nTCxE5bYjYs79maz5X+pFbx+n9NfRMYTkH2CUZ7PawX+D5fGyYl5pDFrzJyXvzBV5AWp2qS63k");
        values.put("onelogin.saml2.security.authnrequest_signed", signed);
        if (signed) {
            values.put("onelogin.saml2.sp.privatekey", SamlPemUtils.normalizePrivateKey(privateKey));
            values.put("onelogin.saml2.sp.x509cert", SamlPemUtils.normalizeCertificate(certificate));
        }
        return values;
    }

    private static String readFixture(String path) {
        try (var stream = SamlAuthnRedirectBuilderTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing fixture: " + path);
            }
            return new String(stream.readAllBytes());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
