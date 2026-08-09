package com.aistudio.api.plugin;

import com.aistudio.api.plugin.dto.InvokeToolRequest;
import com.aistudio.api.plugin.dto.InvokeToolResponse;
import com.aistudio.api.plugin.dto.OrgPluginResponse;
import com.aistudio.api.plugin.dto.PluginResponse;
import com.aistudio.api.plugin.dto.SetPluginEnabledRequest;
import com.aistudio.application.plugin.PluginPackService;
import com.aistudio.application.plugin.PluginService;
import com.aistudio.api.plugin.dto.PluginPackResponse;
import com.aistudio.api.plugin.dto.OrgPluginPackResponse;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Plugins")
public class PluginController {

    private final PluginService pluginService;
    private final PluginPackService pluginPackService;

    public PluginController(PluginService pluginService, PluginPackService pluginPackService) {
        this.pluginService = pluginService;
        this.pluginPackService = pluginPackService;
    }

    @GetMapping("/api/v1/plugins/marketplace")
    @Operation(summary = "List marketplace plugin packs")
    public List<PluginPackResponse> marketplace() {
        return pluginPackService.listMarketplace();
    }

    @GetMapping("/api/v1/organizations/{orgId}/plugin-packs")
    @Operation(summary = "List marketplace packs with install status")
    public List<OrgPluginPackResponse> listOrgPacks(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return pluginPackService.listForOrganization(orgId, user.getId());
    }

    @PostMapping("/api/v1/organizations/{orgId}/plugin-packs/{packId}/install")
    @Operation(summary = "Install a marketplace pack (OWNER)")
    public OrgPluginPackResponse installPack(
            @PathVariable UUID orgId,
            @PathVariable String packId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return pluginPackService.install(orgId, user.getId(), packId);
    }

    @DeleteMapping("/api/v1/organizations/{orgId}/plugin-packs/{packId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Uninstall a marketplace pack (OWNER)")
    public void uninstallPack(
            @PathVariable UUID orgId,
            @PathVariable String packId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        pluginPackService.uninstall(orgId, user.getId(), packId);
    }

    @GetMapping("/api/v1/plugins")
    @Operation(summary = "List discovered plugins (assistant/tool SPI catalog)")
    public List<PluginResponse> catalog() {
        return pluginService.listCatalog();
    }

    @GetMapping("/api/v1/organizations/{orgId}/plugins")
    @Operation(summary = "List plugins with organization enablement")
    public List<OrgPluginResponse> listOrg(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return pluginService.listForOrganization(orgId, user.getId());
    }

    @PutMapping("/api/v1/organizations/{orgId}/plugins/{pluginId}")
    @Operation(summary = "Enable or disable a non-builtin plugin for an organization")
    public OrgPluginResponse setEnabled(
            @PathVariable UUID orgId,
            @PathVariable String pluginId,
            @Valid @RequestBody SetPluginEnabledRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return pluginService.setEnabled(orgId, user.getId(), pluginId, request.enabled());
    }

    @PostMapping("/api/v1/projects/{projectId}/tools/{toolId}/invoke")
    @Operation(summary = "Invoke a tool plugin against a project")
    public InvokeToolResponse invoke(
            @PathVariable UUID projectId,
            @PathVariable String toolId,
            @RequestBody(required = false) InvokeToolRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return pluginService.invokeTool(projectId, user.getId(), toolId, request);
    }
}
