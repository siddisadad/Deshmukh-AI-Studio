package com.aistudio.application.auth;

import com.aistudio.domain.auth.OrgSsoIdpProtocol;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.auth.OidcDiscoveryClient;
import com.aistudio.infrastructure.auth.OidcDiscoveryDocument;
import com.aistudio.infrastructure.auth.SamlIdpMetadataLoader;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsoMetadataRefreshService {

    private final OrgSsoIdpRepository idpRepository;
    private final OidcDiscoveryClient discoveryClient;
    private final SamlIdpMetadataLoader metadataLoader;
    private final ObjectMapper objectMapper;

    public SsoMetadataRefreshService(
            OrgSsoIdpRepository idpRepository,
            OidcDiscoveryClient discoveryClient,
            SamlIdpMetadataLoader metadataLoader,
            ObjectMapper objectMapper
    ) {
        this.idpRepository = idpRepository;
        this.discoveryClient = discoveryClient;
        this.metadataLoader = metadataLoader;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrgSsoIdpEntity refreshMetadata(OrgSsoIdpEntity idp) {
        try {
            if (idp.getProtocol() == OrgSsoIdpProtocol.OIDC) {
                if (idp.getIssuerUri() == null || idp.getIssuerUri().isBlank()) {
                    throw new DomainException("VALIDATION_ERROR", "OIDC issuer URI is required");
                }
                discoveryClient.evict(idp.getIssuerUri());
                OidcDiscoveryDocument document = discoveryClient.discoverByIssuer(idp.getIssuerUri());
                idp.setMetadataJson(objectMapper.writeValueAsString(document));
            } else if (idp.getProtocol() == OrgSsoIdpProtocol.SAML) {
                if (idp.getMetadataUrl() == null || idp.getMetadataUrl().isBlank()) {
                    throw new DomainException("VALIDATION_ERROR", "SAML metadata URL is required");
                }
                SamlIdpMetadataLoader.SamlIdpMetadata metadata = metadataLoader.load(idp.getMetadataUrl());
                idp.setMetadataJson(objectMapper.writeValueAsString(metadata));
            }
            idp.setMetadataFetchedAt(Instant.now());
            idp.setMetadataRefreshError(null);
        } catch (DomainException ex) {
            idp.setMetadataRefreshError(trimError(ex.getMessage()));
            throw ex;
        } catch (Exception ex) {
            idp.setMetadataRefreshError(trimError(ex.getMessage()));
            throw new DomainException("CONFIG_ERROR", "Metadata refresh failed: " + ex.getMessage());
        }
        return idpRepository.save(idp);
    }

    @Transactional
    public int refreshAllEnabled() {
        int refreshed = 0;
        for (OrgSsoIdpEntity idp : idpRepository.findByEnabledTrueOrderByDisplayNameAsc()) {
            try {
                refreshMetadata(idp);
                refreshed++;
            } catch (Exception ignored) {
                // error recorded on entity
            }
        }
        return refreshed;
    }

    private static String trimError(String message) {
        if (message == null) {
            return "refresh failed";
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }
}
