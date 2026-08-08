package com.aistudio.infrastructure.auth;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.SsoProperties;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("'${aistudio.sso.provider:mock}' == 'saml' && '${aistudio.sso.saml.stub-mode:true}' == 'false'")
public class SamlSettingsService {

    private static final Duration METADATA_TTL = Duration.ofHours(1);

    private final SsoProperties ssoProperties;
    private final SamlIdpMetadataLoader metadataLoader;
    private final AtomicReference<CachedSettings> cached = new AtomicReference<>();

    SamlSettingsService(SsoProperties ssoProperties, SamlIdpMetadataLoader metadataLoader) {
        this.ssoProperties = ssoProperties;
        this.metadataLoader = metadataLoader;
        if (!ssoProperties.saml().configured()) {
            throw new IllegalStateException(
                    "aistudio.sso.provider=saml with SAML_STUB_MODE=false requires "
                            + "SAML_METADATA_URL, SAML_ENTITY_ID, and SAML_ACS_URL"
            );
        }
    }

    Saml2Settings settings() {
        CachedSettings current = cached.get();
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.settings();
        }
        SamlIdpMetadataLoader.SamlIdpMetadata idp = metadataLoader.load(ssoProperties.saml().metadataUrl());
        Saml2Settings built = buildSettings(idp);
        cached.set(new CachedSettings(built, Instant.now().plus(METADATA_TTL)));
        return built;
    }

    public String spMetadataXml() {
        try {
            return settings().getSPMetadata();
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Failed to generate SP metadata");
        }
    }

    private Saml2Settings buildSettings(SamlIdpMetadataLoader.SamlIdpMetadata idp) {
        Map<String, Object> values = new HashMap<>();
        values.put("onelogin.saml2.strict", true);
        values.put("onelogin.saml2.debug", false);
        values.put("onelogin.saml2.sp.entityid", ssoProperties.saml().entityId().trim());
        values.put("onelogin.saml2.sp.assertion_consumer_service.url", ssoProperties.saml().acsUrl().trim());
        values.put("onelogin.saml2.idp.entityid", idp.entityId());
        values.put("onelogin.saml2.idp.single_sign_on_service.url", idp.singleSignOnUrl());
        values.put("onelogin.saml2.idp.x509cert", idp.signingCertificate());
        values.put("onelogin.saml2.security.authnrequest_signed", false);
        values.put("onelogin.saml2.security.want_messages_signed", false);
        values.put("onelogin.saml2.security.want_assertions_signed", true);
        values.put("onelogin.saml2.security.want_nameid_encrypted", false);
        try {
            SettingsBuilder builder = new SettingsBuilder();
            builder.fromValues(values);
            return builder.build();
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Failed to build SAML settings");
        }
    }

    private record CachedSettings(Saml2Settings settings, Instant expiresAt) {
    }
}
