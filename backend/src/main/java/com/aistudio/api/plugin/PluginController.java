package com.aistudio.api.plugin;

import com.aistudio.api.plugin.dto.InvokeToolRequest;
import com.aistudio.api.plugin.dto.InvokeToolResponse;
import com.aistudio.api.plugin.dto.OrgPluginResponse;
import com.aistudio.api.plugin.dto.PluginResponse;
import com.aistudio.api.plugin.dto.SetPluginEnabledRequest;
import com.aistudio.application.plugin.PluginService;
import com.aistudio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Plugins")
public class PluginController {

    private final PluginService pluginService;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
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
