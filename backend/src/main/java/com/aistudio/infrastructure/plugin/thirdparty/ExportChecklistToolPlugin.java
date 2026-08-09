package com.aistudio.infrastructure.plugin.thirdparty;

import com.aistudio.application.plugin.spi.ToolPlugin;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExportChecklistToolPlugin implements ToolPlugin {

  private static final List<String> CHECKLIST = List.of(
          "Watermark enabled for external shares",
          "DLP scan reviewed for sensitive exports",
          "Retention policy acknowledged",
          "Legal hold status verified"
  );

    @Override public String id() { return "thirdparty.tool.export_checklist"; }
    @Override public String name() { return "Export Checklist"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() {
        return "Returns a compliance export checklist (third-party compliance pack).";
    }
    @Override public boolean builtin() { return false; }
    @Override public String inputSchemaDescription() {
        return "No arguments required.";
    }

    @Override
    public ToolResult invoke(ToolContext context) {
        String body = String.join("\n", CHECKLIST);
        return new ToolResult(true, body, Map.of("plugin", id(), "items", CHECKLIST.size()));
    }
}
