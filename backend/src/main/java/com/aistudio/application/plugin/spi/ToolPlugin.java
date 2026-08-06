package com.aistudio.application.plugin.spi;

import com.aistudio.domain.plugin.PluginType;
import java.util.Map;
import java.util.UUID;

public interface ToolPlugin extends StudioPlugin {

    String inputSchemaDescription();

    ToolResult invoke(ToolContext context);

    @Override
    default PluginType type() {
        return PluginType.TOOL;
    }

    record ToolContext(
            UUID organizationId,
            UUID projectId,
            UUID userId,
            Map<String, Object> arguments
    ) {
    }

    record ToolResult(
            boolean success,
            String output,
            Map<String, Object> metadata
    ) {
        public static ToolResult ok(String output) {
            return new ToolResult(true, output, Map.of());
        }

        public static ToolResult fail(String message) {
            return new ToolResult(false, message, Map.of());
        }
    }
}
