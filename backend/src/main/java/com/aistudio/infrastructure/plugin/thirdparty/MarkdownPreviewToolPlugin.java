package com.aistudio.infrastructure.plugin.thirdparty;

import com.aistudio.application.plugin.spi.ToolPlugin;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarkdownPreviewToolPlugin implements ToolPlugin {

    @Override public String id() { return "thirdparty.tool.markdown_preview"; }
    @Override public String name() { return "Markdown Preview"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() {
        return "Renders a short markdown snippet as plain-text preview (third-party devtools pack).";
    }
    @Override public boolean builtin() { return false; }
    @Override public String inputSchemaDescription() {
        return "arguments.markdown (string) — markdown source to preview.";
    }

    @Override
    public ToolResult invoke(ToolContext context) {
        Object raw = context.arguments() == null ? null : context.arguments().get("markdown");
        String markdown = raw == null ? "" : String.valueOf(raw);
        if (markdown.isBlank()) {
            return ToolResult.fail("markdown argument is required");
        }
        String preview = markdown
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replaceAll("^#+\\s*", "")
                .trim();
        if (preview.length() > 500) {
            preview = preview.substring(0, 497) + "...";
        }
        return new ToolResult(true, preview, Map.of("plugin", id(), "chars", markdown.length()));
    }
}
