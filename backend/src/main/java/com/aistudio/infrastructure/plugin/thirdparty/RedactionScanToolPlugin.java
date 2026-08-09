package com.aistudio.infrastructure.plugin.thirdparty;

import com.aistudio.application.plugin.spi.ToolPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RedactionScanToolPlugin implements ToolPlugin {

    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    @Override public String id() { return "thirdparty.tool.redaction_scan"; }
    @Override public String name() { return "Redaction Scan"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() {
        return "Lightweight PII pattern scan for compliance review (third-party compliance pack).";
    }
    @Override public boolean builtin() { return false; }
    @Override public String inputSchemaDescription() {
        return "arguments.text (string) — content to scan for email/SSN patterns.";
    }

    @Override
    public ToolResult invoke(ToolContext context) {
        Object raw = context.arguments() == null ? null : context.arguments().get("text");
        String text = raw == null ? "" : String.valueOf(raw);
        if (text.isBlank()) {
            return ToolResult.fail("text argument is required");
        }
        List<String> hits = new ArrayList<>();
        if (EMAIL.matcher(text).find()) {
            hits.add("email");
        }
        if (SSN.matcher(text).find()) {
            hits.add("ssn");
        }
        String summary = hits.isEmpty() ? "no patterns detected" : "patterns: " + String.join(", ", hits);
        return new ToolResult(true, summary, Map.of("plugin", id(), "hits", hits));
    }
}
