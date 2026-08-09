package com.aistudio.infrastructure.plugin.thirdparty;

import com.aistudio.application.plugin.spi.ToolPlugin;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WordCountToolPlugin implements ToolPlugin {

    @Override public String id() { return "thirdparty.tool.word_count"; }
    @Override public String name() { return "Word Count"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() {
        return "Counts words and lines in a text blob (third-party devtools pack).";
    }
    @Override public boolean builtin() { return false; }
    @Override public String inputSchemaDescription() {
        return "arguments.text (string) — content to analyze.";
    }

    @Override
    public ToolResult invoke(ToolContext context) {
        Object raw = context.arguments() == null ? null : context.arguments().get("text");
        String text = raw == null ? "" : String.valueOf(raw);
        if (text.isBlank()) {
            return ToolResult.fail("text argument is required");
        }
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        int lines = text.split("\\R", -1).length;
        return new ToolResult(
                true,
                "words=" + words + ", lines=" + lines,
                Map.of("plugin", id(), "words", words, "lines", lines));
    }
}
