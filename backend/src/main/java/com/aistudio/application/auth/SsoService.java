package com.aistudio.application.auth;

import com.aistudio.api.auth.dto.SsoCallbackRequest;
import com.aistudio.api.auth.dto.SsoProviderResponse;
import com.aistudio.api.auth.dto.SsoStartRequest;
import com.aistudio.api.auth.dto.SsoStartResponse;
import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.auth.SsoPortRegistry;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsoService {

    private final SsoPortRegistry ssoPortRegistry;
    private final AuthService authService;
    private final OrganizationRepository organizationRepository;

    public SsoService(
            SsoPortRegistry ssoPortRegistry,
            AuthService authService,
            OrganizationRepository organizationRepository
    ) {
        this.ssoPortRegistry = ssoPortRegistry;
        this.authService = authService;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public List<SsoProviderResponse> listProviders(Optional<UUID> organizationId, Optional<String> organizationSlug) {
        UUID resolvedOrgId = resolveOrganizationId(organizationId, organizationSlug);
        return SsoPort.toProviderInfos(ssoPortRegistry.listPorts(Optional.ofNullable(resolvedOrgId))).stream()
                .map(p -> new SsoProviderResponse(p.id(), p.displayName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SsoStartResponse start(SsoStartRequest request) {
        SsoPort port = ssoPortRegistry.requireProvider(request.provider());
        String state = UUID.randomUUID().toString().replace("-", "");
        SsoPort.AuthorizationStart started = port.startAuthorization(
                request.redirectUri(),
                state,
                request.loginHint()
        );
        return new SsoStartResponse(port.providerId(), started.authorizationUrl(), started.state());
    }

    @Transactional
    public TokenResponse complete(SsoCallbackRequest request, String ip) {
        SsoPort port = ssoPortRegistry.requireProvider(request.provider());
        SsoPort.UserInfo info = port.exchangeCode(request.code(), request.redirectUri(), request.state());
        return authService.loginWithExternalIdentity(
                port.providerId(),
                info.subject(),
                info.email(),
                info.displayName(),
                info.emailVerified(),
                ip
        );
    }

    private UUID resolveOrganizationId(Optional<UUID> organizationId, Optional<String> organizationSlug) {
        if (organizationId.isPresent()) {
            return organizationId.get();
        }
        if (organizationSlug.isPresent() && !organizationSlug.get().isBlank()) {
            return organizationRepository.findBySlug(organizationSlug.get().trim())
                    .map(org -> org.getId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Organization not found"));
        }
        return null;
    }
}
