package com.aistudio.application.ai;

import com.aistudio.application.plugin.PluginRegistry;
import com.aistudio.application.plugin.spi.AssistantPlugin;
import com.aistudio.domain.ai.AssistantRole;
import com.aistudio.domain.common.DomainException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AssistantRegistry {

    public record AssistantDefinition(
            AssistantRole role,
            String pluginId,
            String name,
            String promptKey,
            List<String> capabilities,
            List<String> limitations,
            List<String> toolIds
    ) {
    }

    private final PluginRegistry pluginRegistry;

    public AssistantRegistry(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    public List<AssistantDefinition> all() {
        return pluginRegistry.assistants().stream().map(this::toDefinition).toList();
    }

    public AssistantDefinition require(AssistantRole role) {
        return toDefinition(pluginRegistry.requireAssistant(role));
    }

    public AssistantRole parseRole(String value) {
        try {
            return Arrays.stream(AssistantRole.values())
                    .filter(r -> r.name().equalsIgnoreCase(value)
                            || r.name().replace("_", "").equalsIgnoreCase(
                                    value.replace("-", "").replace("_", "")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown assistant role: " + value));
        } catch (IllegalArgumentException ex) {
            throw new DomainException("VALIDATION_ERROR", ex.getMessage());
        }
    }

    private AssistantDefinition toDefinition(AssistantPlugin plugin) {
        return new AssistantDefinition(
                plugin.role(),
                plugin.id(),
                plugin.name(),
                plugin.promptKey(),
                plugin.capabilities(),
                plugin.limitations(),
                plugin.toolIds()
        );
    }
}
