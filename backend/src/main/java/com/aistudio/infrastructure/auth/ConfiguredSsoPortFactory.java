package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.auth.OrgSsoIdpProtocol;
import com.aistudio.infrastructure.config.SsoProperties;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ConfiguredSsoPortFactory {

    private final OrgSsoIdpRepository idpRepository;
    private final OidcDiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final SsoPendingAuthStore pendingStore;
    private final SsoProperties ssoProperties;
    private final ObjectMapper objectMapper;

    public ConfiguredSsoPortFactory(
            OrgSsoIdpRepository idpRepository,
            OidcDiscoveryClient discoveryClient,
            RestClient.Builder restClientBuilder,
            SsoPendingAuthStore pendingStore,
            SsoProperties ssoProperties,
            ObjectMapper objectMapper
    ) {
        this.idpRepository = idpRepository;
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
        this.pendingStore = pendingStore;
        this.ssoProperties = ssoProperties;
        this.objectMapper = objectMapper;
    }

    public SsoPort create(OrgSsoIdpEntity idp) {
        if (idp.getProtocol() == OrgSsoIdpProtocol.OIDC) {
            return new OidcConfiguredSsoPort(
                    idp, idpRepository, discoveryClient, restClient, pendingStore, ssoProperties);
        }
        return new SamlConfiguredSsoPort(
                idp, idpRepository, pendingStore, ssoProperties, objectMapper);
    }
}
