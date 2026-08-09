package com.aistudio.infrastructure.auth;

import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SamlAcsCompletionService {

    private final SsoPendingAuthStore pendingStore;
    private final OrgSsoIdpRepository idpRepository;
    private final ConfiguredSsoPortFactory portFactory;
    private final Optional<SamlSpSsoAdapter> samlSpSsoAdapter;

    public SamlAcsCompletionService(
            SsoPendingAuthStore pendingStore,
            OrgSsoIdpRepository idpRepository,
            ConfiguredSsoPortFactory portFactory,
            Optional<SamlSpSsoAdapter> samlSpSsoAdapter
    ) {
        this.pendingStore = pendingStore;
        this.idpRepository = idpRepository;
        this.portFactory = portFactory;
        this.samlSpSsoAdapter = samlSpSsoAdapter;
    }

    public String complete(String samlResponse, String relayState) {
        Optional<SsoPendingAuthStore.PendingSamlStart> configured = pendingStore.peekSamlStart(relayState);
        if (configured.isPresent()) {
            OrgSsoIdpEntity idp = idpRepository.findById(configured.get().idpId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO IdP configuration not found"));
            SamlConfiguredSsoPort port = (SamlConfiguredSsoPort) portFactory.create(idp);
            return port.completeAcs(samlResponse, relayState);
        }
        return samlSpSsoAdapter
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SAML SP mode is not enabled"))
                .completeAcs(samlResponse, relayState);
    }
}
