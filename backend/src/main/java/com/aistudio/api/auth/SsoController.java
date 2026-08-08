package com.aistudio.api.auth;

import com.aistudio.api.auth.dto.SsoCallbackRequest;
import com.aistudio.api.auth.dto.SsoProviderResponse;
import com.aistudio.api.auth.dto.SsoStartRequest;
import com.aistudio.api.auth.dto.SsoStartResponse;
import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.application.auth.SsoService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.auth.SamlSettingsService;
import com.aistudio.infrastructure.auth.SamlSpSsoAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/sso")
@Tag(name = "SSO")
public class SsoController {

    private final SsoService ssoService;
    private final Optional<SamlSpSsoAdapter> samlSpSsoAdapter;
    private final Optional<SamlSettingsService> samlSettingsService;

    public SsoController(
            SsoService ssoService,
            Optional<SamlSpSsoAdapter> samlSpSsoAdapter,
            Optional<SamlSettingsService> samlSettingsService
    ) {
        this.ssoService = ssoService;
        this.samlSpSsoAdapter = samlSpSsoAdapter;
        this.samlSettingsService = samlSettingsService;
    }

    @GetMapping("/providers")
    @Operation(summary = "List enabled SSO providers")
    public List<SsoProviderResponse> providers() {
        return ssoService.listProviders();
    }

    @PostMapping("/start")
    @Operation(summary = "Start SSO authorization (returns redirect URL)")
    public SsoStartResponse start(@Valid @RequestBody SsoStartRequest request) {
        return ssoService.start(request);
    }

    @PostMapping("/callback")
    @Operation(summary = "Complete SSO login with authorization code")
    public TokenResponse callback(@Valid @RequestBody SsoCallbackRequest request, HttpServletRequest http) {
        return ssoService.complete(request, clientIp(http));
    }

    @PostMapping(value = "/saml/acs", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "SAML assertion consumer (HTTP-POST binding from IdP)")
    public void samlAcs(
            @RequestParam("SAMLResponse") String samlResponse,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletResponse response
    ) throws java.io.IOException {
        if (relayState == null || relayState.isBlank()) {
            throw new DomainException("INVALID_TOKEN", "RelayState is required");
        }
        String redirectUrl = samlSpSsoAdapter
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SAML SP mode is not enabled"))
                .completeAcs(samlResponse, relayState);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping(value = "/saml/metadata", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "SP metadata XML for IdP configuration")
    public String samlMetadata() {
        return samlSettingsService
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SAML SP mode is not enabled"))
                .spMetadataXml();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
