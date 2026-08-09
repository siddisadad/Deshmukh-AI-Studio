package com.aistudio.infrastructure.auth;

import com.aistudio.application.auth.SsoPort;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Shared pending-auth state for configured (DB-backed) SSO IdPs.
 */
@Component
class SsoPendingAuthStore {

    private final Map<String, PendingOidcStart> oidcStarts = new ConcurrentHashMap<>();
    private final Map<String, PendingSamlStart> samlStarts = new ConcurrentHashMap<>();
    private final Map<String, PendingSamlExchange> samlExchanges = new ConcurrentHashMap<>();

    void putOidcStart(String state, UUID idpId, String redirectUri, long expiresAtMs) {
        oidcStarts.put(state, new PendingOidcStart(idpId, redirectUri, expiresAtMs));
    }

    Optional<PendingOidcStart> removeOidcStart(String state) {
        return Optional.ofNullable(oidcStarts.remove(state));
    }

    void putSamlStart(String state, UUID idpId, String redirectUri, String requestId, long expiresAtMs) {
        samlStarts.put(state, new PendingSamlStart(idpId, redirectUri, requestId, expiresAtMs));
    }

    Optional<PendingSamlStart> removeSamlStart(String state) {
        return Optional.ofNullable(samlStarts.remove(state));
    }

    Optional<PendingSamlStart> peekSamlStart(String state) {
        return Optional.ofNullable(samlStarts.get(state));
    }

    void putSamlExchange(String code, SsoPort.UserInfo userInfo, long expiresAtMs) {
        samlExchanges.put(code, new PendingSamlExchange(userInfo, expiresAtMs));
    }

    Optional<PendingSamlExchange> removeSamlExchange(String code) {
        return Optional.ofNullable(samlExchanges.remove(code));
    }

    record PendingOidcStart(UUID idpId, String redirectUri, long expiresAtMs) {
    }

    record PendingSamlStart(UUID idpId, String redirectUri, String requestId, long expiresAtMs) {
    }

    record PendingSamlExchange(SsoPort.UserInfo userInfo, long expiresAtMs) {
    }
}
