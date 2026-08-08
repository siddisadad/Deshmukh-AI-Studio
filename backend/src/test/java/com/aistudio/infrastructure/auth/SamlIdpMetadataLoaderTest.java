package com.aistudio.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aistudio.domain.common.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SamlIdpMetadataLoaderTest {

    @Test
    void parsesIdpMetadataXml() {
        SamlIdpMetadataLoader loader = new SamlIdpMetadataLoader(RestClient.builder());
        String xml = readFixture("saml/idp-metadata.xml");
        SamlIdpMetadataLoader.SamlIdpMetadata metadata = loader.parse(xml);

        assertEquals("https://idp.example.com/metadata", metadata.entityId());
        assertEquals("https://idp.example.com/sso/redirect", metadata.singleSignOnUrl());
        assertEquals(false, metadata.signingCertificate().contains(" "));
    }

    @Test
    void rejectsMissingEntityId() {
        SamlIdpMetadataLoader loader = new SamlIdpMetadataLoader(RestClient.builder());
        assertThrows(DomainException.class, () -> loader.parse("<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"/>"));
    }

    private static String readFixture(String path) {
        try (var stream = SamlIdpMetadataLoaderTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing fixture: " + path);
            }
            return new String(stream.readAllBytes());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
