package com.aistudio.api.organization;

import com.aistudio.api.organization.dto.AddMemberRequest;
import com.aistudio.api.organization.dto.MemberResponse;
import com.aistudio.api.organization.dto.OrganizationResponse;
import com.aistudio.api.organization.dto.OrgAiCanaryEvaluationResponse;
import com.aistudio.api.organization.dto.OrgAiPolicyChangeResponse;
import com.aistudio.api.organization.dto.OrgAiPolicyResponse;
import com.aistudio.api.organization.dto.OrgAiPolicySimulationRecordResponse;
import com.aistudio.api.organization.dto.OrgAiPolicySimulationResponse;
import com.aistudio.api.organization.dto.UpdateOrgAiCanaryHooksRequest;
import com.aistudio.api.organization.dto.UpdateOrgAiCanaryRequest;
import com.aistudio.api.organization.dto.UpdateOrgAiPolicyRequest;
import com.aistudio.api.organization.dto.OrgSloSettingsResponse;
import com.aistudio.api.organization.dto.OrgSsoIdpResponse;
import com.aistudio.api.organization.dto.CreateOrgSsoIdpRequest;
import com.aistudio.api.organization.dto.UpdateOrgSsoIdpRequest;
import com.aistudio.api.organization.dto.UpdateOrgSloSettingsRequest;
import com.aistudio.application.organization.OrgSloService;
import com.aistudio.api.organization.dto.CreateOrgDlpConnectorRequest;
import com.aistudio.api.organization.dto.OrgDlpConnectorResponse;
import com.aistudio.api.organization.dto.ThreadExportDlpEventResponse;
import com.aistudio.api.codemetadata.dto.GitConnectionTestResponse;
import com.aistudio.api.organization.dto.OrgGitCredentialEventResponse;
import com.aistudio.api.organization.dto.OrgGitCredentialResponse;
import com.aistudio.api.organization.dto.UpsertOrgGitCredentialRequest;
import com.aistudio.application.organization.OrgDlpConnectorService;
import com.aistudio.application.organization.OrgGitCredentialService;
import com.aistudio.application.organization.OrgGitSyncRunsService;
import com.aistudio.application.organization.OrgGitSyncOverviewService;
import com.aistudio.api.organization.dto.OrgGitSyncRunExport;
import com.aistudio.api.organization.dto.OrgGitSyncRunPageResponse;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewExport;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRetryFailedResponse;
import com.aistudio.application.organization.OrgSsoIdpService;
import com.aistudio.application.project.OrgAiPolicyService;
import com.aistudio.application.project.OrganizationService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final OrgSloService orgSloService;
    private final OrgSsoIdpService orgSsoIdpService;
    private final OrgDlpConnectorService orgDlpConnectorService;
    private final OrgGitCredentialService orgGitCredentialService;
    private final OrgGitSyncOverviewService orgGitSyncOverviewService;
    private final OrgGitSyncRunsService orgGitSyncRunsService;

    public OrganizationController(
            OrganizationService organizationService,
            OrgAiPolicyService orgAiPolicyService,
            OrgSloService orgSloService,
            OrgSsoIdpService orgSsoIdpService,
            OrgDlpConnectorService orgDlpConnectorService,
            OrgGitCredentialService orgGitCredentialService,
            OrgGitSyncOverviewService orgGitSyncOverviewService,
            OrgGitSyncRunsService orgGitSyncRunsService
    ) {
        this.organizationService = organizationService;
        this.orgAiPolicyService = orgAiPolicyService;
        this.orgSloService = orgSloService;
        this.orgSsoIdpService = orgSsoIdpService;
        this.orgDlpConnectorService = orgDlpConnectorService;
        this.orgGitCredentialService = orgGitCredentialService;
        this.orgGitSyncOverviewService = orgGitSyncOverviewService;
        this.orgGitSyncRunsService = orgGitSyncRunsService;
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

    @GetMapping("/{orgId}/ai-policy/simulations")
    @Operation(summary = "List AI routing policy simulation audit trail")
    public List<OrgAiPolicySimulationRecordResponse> listAiPolicySimulations(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.listSimulations(orgId, user.getId(), limit);
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

    @PutMapping("/{orgId}/ai-policy/canary")
    @Operation(summary = "Start or update canary provider chain rollout (OWNER/ADMIN)")
    public OrgAiPolicyResponse updateAiPolicyCanary(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrgAiCanaryRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.updateCanary(orgId, user.getId(), request);
    }

    @PostMapping("/{orgId}/ai-policy/canary/promote")
    @Operation(summary = "Promote canary provider chain to stable policy (OWNER/ADMIN)")
    public OrgAiPolicyResponse promoteAiPolicyCanary(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.promoteCanary(orgId, user.getId());
    }

    @DeleteMapping("/{orgId}/ai-policy/canary")
    @Operation(summary = "Abort canary rollout without promoting (OWNER/ADMIN)")
    public OrgAiPolicyResponse abortAiPolicyCanary(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.abortCanary(orgId, user.getId());
    }

    @PutMapping("/{orgId}/ai-policy/canary/hooks")
    @Operation(summary = "Configure automated canary promotion / rollback hooks (OWNER)")
    public OrgAiPolicyResponse updateAiPolicyCanaryHooks(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrgAiCanaryHooksRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.updateCanaryHooks(orgId, user.getId(), request);
    }

    @PostMapping("/{orgId}/ai-policy/canary/evaluate")
    @Operation(summary = "Evaluate canary metrics and apply auto promote/abort hooks (OWNER)")
    public OrgAiCanaryEvaluationResponse evaluateAiPolicyCanaryHooks(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgAiPolicyService.evaluateCanaryHooks(orgId, user.getId());
    }

    @GetMapping("/{orgId}/slo")
    @Operation(summary = "Get organization SLO targets")
    public OrgSloSettingsResponse getOrgSloSettings(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgSloService.getSettings(orgId, user.getId());
    }

    @PutMapping("/{orgId}/slo")
    @Operation(summary = "Update organization SLO targets (OWNER)")
    public OrgSloSettingsResponse updateOrgSloSettings(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrgSloSettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgSloService.updateSettings(orgId, user.getId(), request);
    }

    @GetMapping("/{orgId}/sso/idps")
    @Operation(summary = "List organization SSO IdP configurations")
    public List<OrgSsoIdpResponse> listOrgSsoIdps(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgSsoIdpService.list(orgId, user.getId());
    }

    @PostMapping("/{orgId}/sso/idps")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create organization SSO IdP (OWNER)")
    public OrgSsoIdpResponse createOrgSsoIdp(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateOrgSsoIdpRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgSsoIdpService.create(orgId, user.getId(), request);
    }

    @PutMapping("/{orgId}/sso/idps/{idpId}")
    @Operation(summary = "Update organization SSO IdP (OWNER)")
    public OrgSsoIdpResponse updateOrgSsoIdp(
            @PathVariable UUID orgId,
            @PathVariable UUID idpId,
            @Valid @RequestBody UpdateOrgSsoIdpRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgSsoIdpService.update(orgId, idpId, user.getId(), request);
    }

    @DeleteMapping("/{orgId}/sso/idps/{idpId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete organization SSO IdP (OWNER)")
    public void deleteOrgSsoIdp(
            @PathVariable UUID orgId,
            @PathVariable UUID idpId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        orgSsoIdpService.delete(orgId, idpId, user.getId());
    }

    @PostMapping("/{orgId}/sso/idps/{idpId}/refresh-metadata")
    @Operation(summary = "Refresh IdP metadata from issuer or federation URL (OWNER)")
    public OrgSsoIdpResponse refreshOrgSsoIdpMetadata(
            @PathVariable UUID orgId,
            @PathVariable UUID idpId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgSsoIdpService.refreshMetadata(orgId, idpId, user.getId());
    }

    @GetMapping("/{orgId}/dlp/connectors")
    @Operation(summary = "List organization DLP connectors")
    public List<OrgDlpConnectorResponse> listDlpConnectors(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgDlpConnectorService.listConnectors(orgId, user.getId());
    }

    @PostMapping("/{orgId}/dlp/connectors")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create DLP connector (OWNER)")
    public OrgDlpConnectorResponse createDlpConnector(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateOrgDlpConnectorRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgDlpConnectorService.createConnector(orgId, user.getId(), request);
    }

    @DeleteMapping("/{orgId}/dlp/connectors/{connectorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete DLP connector (OWNER)")
    public void deleteDlpConnector(
            @PathVariable UUID orgId,
            @PathVariable UUID connectorId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        orgDlpConnectorService.deleteConnector(orgId, connectorId, user.getId());
    }

    @GetMapping("/{orgId}/dlp/events")
    @Operation(summary = "List thread export DLP events")
    public List<ThreadExportDlpEventResponse> listDlpEvents(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgDlpConnectorService.listEvents(orgId, user.getId());
    }

    @GetMapping("/{orgId}/git-credentials/events")
    @Operation(summary = "List organization git credential rotation audit events")
    public List<OrgGitCredentialEventResponse> listGitCredentialEvents(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitCredentialService.listEvents(orgId, user.getId(), limit);
    }

    @GetMapping("/{orgId}/git-credentials")
    @Operation(summary = "List organization git host credentials")
    public List<OrgGitCredentialResponse> listGitCredentials(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitCredentialService.list(orgId, user.getId());
    }

    @PutMapping("/{orgId}/git-credentials/{provider}")
    @Operation(summary = "Upsert organization git credential (OWNER)")
    public OrgGitCredentialResponse upsertGitCredential(
            @PathVariable UUID orgId,
            @PathVariable String provider,
            @Valid @RequestBody UpsertOrgGitCredentialRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitCredentialService.upsert(orgId, provider, user.getId(), request);
    }

    @DeleteMapping("/{orgId}/git-credentials/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete organization git credential (OWNER)")
    public void deleteGitCredential(
            @PathVariable UUID orgId,
            @PathVariable String provider,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        orgGitCredentialService.delete(orgId, provider, user.getId());
    }

    @PostMapping("/{orgId}/git-credentials/{provider}/test")
    @Operation(summary = "Test organization git credential (OWNER)")
    public GitConnectionTestResponse testGitCredential(
            @PathVariable UUID orgId,
            @PathVariable String provider,
            @RequestParam(required = false) String repository,
            @RequestParam(required = false) String branch,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitCredentialService.test(orgId, provider, user.getId(), repository, branch);
    }

    @GetMapping("/{orgId}/git-sync-overview")
    @Operation(summary = "Organization git sync overview across projects")
    public OrgGitSyncOverviewResponse getGitSyncOverview(
            @PathVariable UUID orgId,
            @RequestParam(required = false) Boolean linked,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String lastSyncStatus,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitSyncOverviewService.getOverview(orgId, user.getId(), linked, provider, lastSyncStatus);
    }

    @GetMapping("/{orgId}/git-sync-overview/export")
    @Operation(summary = "Export organization git sync overview as CSV or JSON")
    public ResponseEntity<byte[]> exportGitSyncOverview(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) Boolean linked,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String lastSyncStatus,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        OrgGitSyncOverviewExport exported = orgGitSyncOverviewService.exportOverview(
                orgId, user.getId(), format, linked, provider, lastSyncStatus);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exported.filename() + "\"")
                .contentType(MediaType.parseMediaType(exported.contentType()))
                .body(exported.body());
    }

    @PostMapping("/{orgId}/git-sync-overview/retry-failed")
    @Operation(summary = "Enqueue background sync for projects with failed last git sync (OWNER/ADMIN)")
    public OrgGitSyncRetryFailedResponse retryFailedGitSyncs(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitSyncOverviewService.retryFailedSyncs(orgId, user.getId());
    }

    @GetMapping("/{orgId}/git-sync-runs")
    @Operation(summary = "Organization git sync run history across projects")
    public OrgGitSyncRunPageResponse listGitSyncRuns(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return orgGitSyncRunsService.listRuns(orgId, user.getId(), limit, offset, source, status, projectId);
    }

    @GetMapping("/{orgId}/git-sync-runs/export")
    @Operation(summary = "Export organization git sync runs as CSV or JSON (up to 1000 rows)")
    public ResponseEntity<byte[]> exportGitSyncRuns(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        OrgGitSyncRunExport exported = orgGitSyncRunsService.exportRuns(
                orgId, user.getId(), format, source, status, projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exported.filename() + "\"")
                .contentType(MediaType.parseMediaType(exported.contentType()))
                .body(exported.body());
    }
}
