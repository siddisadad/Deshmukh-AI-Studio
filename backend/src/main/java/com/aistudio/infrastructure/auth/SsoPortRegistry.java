package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SsoPortRegistry {

    private final List<SsoPort> envPorts;
    private final OrgSsoIdpRepository idpRepository;
    private final ConfiguredSsoPortFactory portFactory;

    public SsoPortRegistry(
            Optional<MockSsoAdapter> mockAdapter,
            Optional<OidcSsoAdapter> oidcAdapter,
            Optional<SamlSsoStubAdapter> samlStubAdapter,
            Optional<SamlSpSsoAdapter> samlSpAdapter,
            OrgSsoIdpRepository idpRepository,
            ConfiguredSsoPortFactory portFactory
    ) {
        this.envPorts = new ArrayList<>();
        mockAdapter.ifPresent(envPorts::add);
        oidcAdapter.ifPresent(envPorts::add);
        samlStubAdapter.ifPresent(envPorts::add);
        samlSpAdapter.ifPresent(envPorts::add);
        this.idpRepository = idpRepository;
        this.portFactory = portFactory;
    }

    public List<SsoPort> listPorts(Optional<UUID> organizationId) {
        List<SsoPort> ports = new ArrayList<>(envPorts);
        List<OrgSsoIdpEntity> configured;
        if (organizationId.isPresent()) {
            configured = idpRepository.findByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(organizationId.get());
        } else {
            configured = List.of();
        }
        for (OrgSsoIdpEntity idp : configured) {
            ports.add(portFactory.create(idp));
        }
        return ports;
    }

    public SsoPort requireProvider(String providerId) {
        String normalized = providerId == null ? "" : providerId.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("db-")) {
            UUID idpId = parseDbProviderId(normalized);
            OrgSsoIdpEntity idp = idpRepository.findById(idpId)
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO provider is not available"));
            SsoPort port = portFactory.create(idp);
            if (!port.enabled()) {
                throw new DomainException("NOT_FOUND", "SSO provider is not available");
            }
            return port;
        }
        return listPorts(Optional.empty()).stream()
                .filter(SsoPort::enabled)
                .filter(port -> port.providerId().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO provider is not available"));
    }

    private static UUID parseDbProviderId(String providerId) {
        try {
            return UUID.fromString(providerId.substring(3));
        } catch (Exception ex) {
            throw new DomainException("NOT_FOUND", "SSO provider is not available");
        }
    }
}
