package com.aistudio.api.profile;

import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.api.profile.dto.ChangePasswordRequest;
import com.aistudio.api.profile.dto.MeResponse;
import com.aistudio.api.profile.dto.UpdateProfileRequest;
import com.aistudio.application.auth.AuthService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Get current user profile")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.me(user.getId());
    }

    @PatchMapping
    @Operation(summary = "Update profile")
    public MeResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateProfile(user.getId(), request);
    }

    @PostMapping("/password")
    @Operation(summary = "Change password (revokes other sessions and returns fresh tokens)")
    public TokenResponse changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return authService.changePassword(user.getId(), request);
    }
}
