package com.aistudio.infrastructure.auth;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import java.util.HashMap;
import java.util.Map;

public final class SamlConfiguredSettingsBuilder {

    private SamlConfiguredSettingsBuilder() {
    }

    public static Saml2Settings build(OrgSsoIdpEntity idp, ObjectMapper objectMapper) {
        if (idp.getEntityId() == null || idp.getEntityId().isBlank()
                || idp.getAcsUrl() == null || idp.getAcsUrl().isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "SAML entity ID and ACS URL are required");
        }
        SamlIdpMetadataLoader.SamlIdpMetadata metadata = resolveMetadata(idp, objectMapper);
        Map<String, Object> values = new HashMap<>();
        values.put("onelogin.saml2.strict", true);
        values.put("onelogin.saml2.debug", false);
        values.put("onelogin.saml2.sp.entityid", idp.getEntityId().trim());
        values.put("onelogin.saml2.sp.assertion_consumer_service.url", idp.getAcsUrl().trim());
        values.put("onelogin.saml2.idp.entityid", metadata.entityId());
        values.put("onelogin.saml2.idp.single_sign_on_service.url", metadata.singleSignOnUrl());
        values.put("onelogin.saml2.idp.x509cert", metadata.signingCertificate());

        boolean signRequests = idp.getSpPrivateKey() != null
                && !idp.getSpPrivateKey().isBlank()
                && idp.getSpCertificate() != null
                && !idp.getSpCertificate().isBlank();
        if (signRequests) {
            values.put("onelogin.saml2.sp.privatekey", SamlPemUtils.normalizePrivateKey(idp.getSpPrivateKey()));
            values.put("onelogin.saml2.sp.x509cert", SamlPemUtils.normalizeCertificate(idp.getSpCertificate()));
            values.put("onelogin.saml2.security.authnrequest_signed", true);
        } else {
            values.put("onelogin.saml2.security.authnrequest_signed", false);
        }

        values.put("onelogin.saml2.security.want_messages_signed", false);
        values.put("onelogin.saml2.security.want_assertions_signed", true);
        values.put("onelogin.saml2.security.want_nameid_encrypted", false);

        boolean wantEncrypted = idp.isWantEncryptedAssertions()
                && metadata.encryptionCertificate() != null
                && !metadata.encryptionCertificate().isBlank();
        values.put("onelogin.saml2.security.want_assertions_encrypted", wantEncrypted);
        if (wantEncrypted) {
            Map<String, String> certMulti = new HashMap<>();
            certMulti.put("signing", metadata.signingCertificate());
            certMulti.put("encryption", metadata.encryptionCertificate());
            values.put("onelogin.saml2.idp.x509certMulti", certMulti);
        }

        try {
            SettingsBuilder builder = new SettingsBuilder();
            builder.fromValues(values);
            return builder.build();
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Failed to build SAML settings");
        }
    }

    private static SamlIdpMetadataLoader.SamlIdpMetadata resolveMetadata(
            OrgSsoIdpEntity idp,
            ObjectMapper objectMapper
    ) {
        if (idp.getMetadataJson() == null || idp.getMetadataJson().isBlank()) {
            throw new DomainException("CONFIG_ERROR", "SAML IdP metadata has not been refreshed");
        }
        try {
            return objectMapper.readValue(idp.getMetadataJson(), SamlIdpMetadataLoader.SamlIdpMetadata.class);
        } catch (Exception ex) {
            throw new DomainException("CONFIG_ERROR", "Cached SAML metadata is invalid");
        }
    }
}
