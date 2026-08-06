package com.aistudio.application.plugin;

import com.aistudio.application.plugin.spi.AssistantPlugin;
import com.aistudio.application.plugin.spi.StudioPlugin;
import com.aistudio.application.plugin.spi.ToolPlugin;
import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrganizationPluginEntity;
import com.aistudio.infrastructure.persistence.entity.PluginEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationPluginRepository;
import com.aistudio.infrastructure.persistence.repository.PluginRepository;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PluginRegistry {

    private final List<StudioPlugin> plugins;
    private final PluginRepository pluginRepository;
    private final OrganizationPluginRepository organizationPluginRepository;
    private Map<String, StudioPlugin> byId = Map.of();
    private Map<AssistantRole, AssistantPlugin> assistantsByRole = Map.of();
    private Map<String, ToolPlugin> toolsById = Map.of();

    public PluginRegistry(
            List<StudioPlugin> plugins,
            PluginRepository pluginRepository,
            OrganizationPluginRepository organizationPluginRepository
    ) {
        this.plugins = plugins;
        this.pluginRepository = pluginRepository;
        this.organizationPluginRepository = organizationPluginRepository;
    }

    @PostConstruct
    void index() {
        byId = plugins.stream().collect(Collectors.toMap(StudioPlugin::id, Function.identity(), (a, b) -> a));
        assistantsByRole = plugins.stream()
                .filter(AssistantPlugin.class::isInstance)
                .map(AssistantPlugin.class::cast)
                .collect(Collectors.toMap(AssistantPlugin::role, Function.identity(), (a, b) -> a));
        toolsById = plugins.stream()
                .filter(ToolPlugin.class::isInstance)
                .map(ToolPlugin.class::cast)
                .collect(Collectors.toMap(ToolPlugin::id, Function.identity(), (a, b) -> a));
    }

    @Transactional
    public void syncCatalog() {
        for (StudioPlugin plugin : plugins) {
            PluginEntity entity = pluginRepository.findById(plugin.id()).orElseGet(PluginEntity::new);
            entity.setId(plugin.id());
            entity.setName(plugin.name());
            entity.setVersion(plugin.version());
            entity.setPluginType(plugin.type());
            entity.setDescription(plugin.description());
            entity.setBuiltin(plugin.builtin());
            entity.setDefaultEnabled(true);
            pluginRepository.save(entity);
        }
    }

    public List<StudioPlugin> all() {
        return plugins.stream()
                .sorted(Comparator.comparing(StudioPlugin::type).thenComparing(StudioPlugin::name))
                .toList();
    }

    public List<AssistantPlugin> assistants() {
        return assistantsByRole.values().stream()
                .sorted(Comparator.comparing(a -> a.role().name()))
                .toList();
    }

    public AssistantPlugin requireAssistant(AssistantRole role) {
        AssistantPlugin plugin = assistantsByRole.get(role);
        if (plugin == null) {
            throw new DomainException("NOT_FOUND", "Unknown assistant: " + role);
        }
        return plugin;
    }

    public ToolPlugin requireTool(String toolId) {
        ToolPlugin tool = toolsById.get(toolId);
        if (tool == null) {
            throw new DomainException("NOT_FOUND", "Unknown tool plugin: " + toolId);
        }
        return tool;
    }

    public StudioPlugin require(String pluginId) {
        StudioPlugin plugin = byId.get(pluginId);
        if (plugin == null) {
            throw new DomainException("NOT_FOUND", "Unknown plugin: " + pluginId);
        }
        return plugin;
    }

    public boolean isEnabled(UUID organizationId, String pluginId) {
        require(pluginId);
        return organizationPluginRepository.findByOrganizationIdAndPluginId(organizationId, pluginId)
                .map(OrganizationPluginEntity::isEnabled)
                .orElse(true);
    }

    public void setEnabled(UUID organizationId, String pluginId, boolean enabled) {
        StudioPlugin plugin = require(pluginId);
        if (plugin.builtin() && !enabled) {
            throw new DomainException("VALIDATION_ERROR", "Built-in plugins cannot be disabled");
        }
        OrganizationPluginEntity row = organizationPluginRepository
                .findByOrganizationIdAndPluginId(organizationId, pluginId)
                .orElseGet(() -> {
                    OrganizationPluginEntity created = new OrganizationPluginEntity();
                    created.setOrganizationId(organizationId);
                    created.setPluginId(pluginId);
                    return created;
                });
        row.setEnabled(enabled);
        organizationPluginRepository.save(row);
    }
}
