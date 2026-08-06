package com.aistudio.application.plugin;

import com.aistudio.api.plugin.dto.InvokeToolRequest;
import com.aistudio.api.plugin.dto.InvokeToolResponse;
import com.aistudio.api.plugin.dto.OrgPluginResponse;
import com.aistudio.api.plugin.dto.PluginResponse;
import com.aistudio.application.plugin.spi.StudioPlugin;
import com.aistudio.application.plugin.spi.ToolPlugin;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrganizationPluginEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationPluginRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PluginService implements ApplicationRunner {

    private final PluginRegistry pluginRegistry;
    private final OrganizationPluginRepository organizationPluginRepository;
    private final ProjectAuthorizationService authorizationService;

    public PluginService(
            PluginRegistry pluginRegistry,
            OrganizationPluginRepository organizationPluginRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.pluginRegistry = pluginRegistry;
        this.organizationPluginRepository = organizationPluginRepository;
        this.authorizationService = authorizationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        pluginRegistry.syncCatalog();
    }

    @Transactional(readOnly = true)
    public List<PluginResponse> listCatalog() {
        return pluginRegistry.all().stream().map(this::toPlugin).toList();
    }

    @Transactional(readOnly = true)
    public List<OrgPluginResponse> listForOrganization(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        Map<String, Boolean> overrides = organizationPluginRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(
                        OrganizationPluginEntity::getPluginId,
                        OrganizationPluginEntity::isEnabled,
                        (a, b) -> a
                ));
        return pluginRegistry.all().stream()
                .map(plugin -> {
                    boolean enabled = overrides.getOrDefault(plugin.id(), true);
                    return new OrgPluginResponse(
                            toPlugin(plugin),
                            enabled,
                            !plugin.builtin()
                    );
                })
                .toList();
    }

    @Transactional
    public OrgPluginResponse setEnabled(UUID organizationId, UUID userId, String pluginId, boolean enabled) {
        authorizationService.requireOrgOwner(organizationId, userId);
        pluginRegistry.setEnabled(organizationId, pluginId, enabled);
        StudioPlugin plugin = pluginRegistry.require(pluginId);
        return new OrgPluginResponse(toPlugin(plugin), enabled, !plugin.builtin());
    }

    @Transactional(readOnly = true)
    public InvokeToolResponse invokeTool(
            UUID projectId,
            UUID userId,
            String toolId,
            InvokeToolRequest request
    ) {
        ProjectEntity project = authorizationService.requireProjectAccess(projectId, userId);
        authorizationService.requireProjectEdit(projectId, userId);
        if (!pluginRegistry.isEnabled(project.getOrganizationId(), toolId)) {
            throw new DomainException("FORBIDDEN", "Tool plugin is disabled for this organization");
        }
        ToolPlugin tool = pluginRegistry.requireTool(toolId);
        Map<String, Object> args = request == null || request.arguments() == null
                ? Map.of()
                : request.arguments();
        ToolPlugin.ToolResult result = tool.invoke(new ToolPlugin.ToolContext(
                project.getOrganizationId(),
                projectId,
                userId,
                args
        ));
        return new InvokeToolResponse(
                tool.id(),
                tool.name(),
                result.success(),
                result.output(),
                result.metadata()
        );
    }

    private PluginResponse toPlugin(StudioPlugin plugin) {
        return new PluginResponse(
                plugin.id(),
                plugin.name(),
                plugin.version(),
                plugin.type().name(),
                plugin.description(),
                plugin.builtin()
        );
    }
}
