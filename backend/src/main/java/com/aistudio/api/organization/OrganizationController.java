package com.aistudio.api.organization;

import com.aistudio.api.organization.dto.AddMemberRequest;
import com.aistudio.api.organization.dto.MemberResponse;
import com.aistudio.api.organization.dto.OrganizationResponse;
import com.aistudio.api.organization.dto.OrgAiPolicyChangeResponse;
import com.aistudio.api.organization.dto.OrgAiPolicyResponse;
import com.aistudio.api.organization.dto.OrgAiPolicySimulationResponse;
import com.aistudio.api.organization.dto.UpdateOrgAiPolicyRequest;
import com.aistudio.application.project.OrgAiPolicyService;
import com.aistudio.application.project.OrganizationService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrgAiPolicyService orgAiPolicyService;

    public OrganizationController(OrganizationService organizationService, OrgAiPolicyService orgAiPolicyService) {
        this.organizationService = organizationService;
        this.orgAiPolicyService = orgAiPolicyService;
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

    @PostMapping("/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an existing user to the organization by email (OWNER/ADMIN)")
    public MemberResponse addMember(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return organizationService.addMember(orgId, user.getId(), request);
    }

    @GetMapping("/{orgId}/ai-policy")
    @Operation(summary = "Get organization AI routing policy and token budget usage")
    public OrgAiPolicyResponse getAiPolicy(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.getPolicy(orgId, user.getId());
    }

    @PutMapping("/{orgId}/ai-policy")
    @Operation(summary = "Update organization AI routing policy (OWNER)")
    public OrgAiPolicyResponse updateAiPolicy(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrgAiPolicyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.updatePolicy(orgId, user.getId(), request);
    }

    @PostMapping("/{orgId}/ai-policy/simulate")
    @Operation(summary = "Dry-run AI routing policy changes without applying them")
    public OrgAiPolicySimulationResponse simulateAiPolicy(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrgAiPolicyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.simulatePolicy(orgId, user.getId(), request);
    }

    @GetMapping("/{orgId}/ai-policy/changes")
    @Operation(summary = "List AI routing policy change audit log")
    public List<OrgAiPolicyChangeResponse> listAiPolicyChanges(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.listChanges(orgId, user.getId(), limit);
    }

    @PostMapping("/{orgId}/ai-policy/pending/approve")
    @Operation(summary = "Approve pending AI routing policy change (OWNER)")
    public OrgAiPolicyResponse approvePendingAiPolicy(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.approvePendingChange(orgId, user.getId());
    }

    @PostMapping("/{orgId}/ai-policy/pending/reject")
    @Operation(summary = "Reject pending AI routing policy change (OWNER)")
    public OrgAiPolicyResponse rejectPendingAiPolicy(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.rejectPendingChange(orgId, user.getId());
    }
}
