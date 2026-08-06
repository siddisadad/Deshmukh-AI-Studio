package com.aistudio.application.auth;

import com.aistudio.api.auth.dto.SsoCallbackRequest;
import com.aistudio.api.auth.dto.SsoProviderResponse;
import com.aistudio.api.auth.dto.SsoStartRequest;
import com.aistudio.api.auth.dto.SsoStartResponse;
import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.domain.common.DomainException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsoService {

    private final List<SsoPort> ssoPorts;
    private final AuthService authService;

    public SsoService(List<SsoPort> ssoPorts, AuthService authService) {
        this.ssoPorts = ssoPorts;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<SsoProviderResponse> listProviders() {
        return SsoPort.toProviderInfos(ssoPorts).stream()
                .map(p -> new SsoProviderResponse(p.id(), p.displayName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SsoStartResponse start(SsoStartRequest request) {
        SsoPort port = requireProvider(request.provider());
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
        SsoPort port = requireProvider(request.provider());
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

    private SsoPort requireProvider(String providerValue) {
        String normalized = providerValue == null ? "" : providerValue.trim().toLowerCase(Locale.ROOT);
        return ssoPorts.stream()
                .filter(SsoPort::enabled)
                .filter(p -> p.providerId().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO provider is not available"));
    }
}
