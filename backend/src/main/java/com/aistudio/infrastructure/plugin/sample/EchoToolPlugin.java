package com.aistudio.infrastructure.plugin.sample;

import com.aistudio.application.plugin.spi.ToolPlugin;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Sample extension tool demonstrating the plugin SPI (can be disabled per org).
 */
@Component
public class EchoToolPlugin implements ToolPlugin {

    @Override public String id() { return "sample.tool.echo"; }
    @Override public String name() { return "Echo (Sample)"; }
    @Override public String version() { return "0.1.0"; }
    @Override public String description() {
        return "Sample extension tool that echoes input. Disable via organization plugins.";
    }
    @Override public boolean builtin() { return false; }
    @Override public String inputSchemaDescription() {
        return "arguments.message (string) — text to echo back.";
    }

    @Override
    public ToolResult invoke(ToolContext context) {
        Object message = context.arguments() == null ? null : context.arguments().get("message");
        String text = message == null || String.valueOf(message).isBlank()
                ? "(empty)"
                : String.valueOf(message);
        return new ToolResult(true, "echo: " + text, Map.of("plugin", id()));
    }
}
