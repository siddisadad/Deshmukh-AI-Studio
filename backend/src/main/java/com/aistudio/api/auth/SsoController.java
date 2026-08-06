package com.aistudio.api.auth;

import com.aistudio.api.auth.dto.SsoCallbackRequest;
import com.aistudio.api.auth.dto.SsoProviderResponse;
import com.aistudio.api.auth.dto.SsoStartRequest;
import com.aistudio.api.auth.dto.SsoStartResponse;
import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.application.auth.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/sso")
@Tag(name = "SSO")
public class SsoController {

    private final SsoService ssoService;

    public SsoController(SsoService ssoService) {
        this.ssoService = ssoService;
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

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
