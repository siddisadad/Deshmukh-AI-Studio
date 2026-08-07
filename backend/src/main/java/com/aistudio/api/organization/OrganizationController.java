package com.aistudio.api.organization;

import com.aistudio.api.organization.dto.MemberResponse;
import com.aistudio.api.organization.dto.OrganizationResponse;
import com.aistudio.application.project.OrganizationService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @Operation(summary = "List organizations for current user")
    public List<OrganizationResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return organizationService.listForUser(user.getId());
    }

    @GetMapping("/{orgId}")
    @Operation(summary = "Get organization")
    public OrganizationResponse get(@PathVariable UUID orgId, @AuthenticationPrincipal AuthenticatedUser user) {
        return organizationService.get(orgId, user.getId());
    }

    @GetMapping("/{orgId}/members")
    @Operation(summary = "List organization members")
    public List<MemberResponse> listMembers(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return organizationService.listMembers(orgId, user.getId());
    }
}
