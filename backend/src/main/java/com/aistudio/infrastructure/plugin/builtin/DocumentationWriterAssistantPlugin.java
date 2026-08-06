package com.aistudio.infrastructure.plugin.builtin;

import com.aistudio.application.plugin.spi.AssistantPlugin;
import com.aistudio.domain.ai.AssistantRole;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentationWriterAssistantPlugin implements AssistantPlugin {
    @Override public String id() { return "core.assistant.documentation_writer"; }
    @Override public String name() { return "Documentation Writer"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() { return "README, API docs, and release notes from project context."; }
    @Override public boolean builtin() { return true; }
    @Override public AssistantRole role() { return AssistantRole.DOCUMENTATION_WRITER; }
    @Override public String promptKey() { return "documentation_writer"; }
    @Override public List<String> capabilities() {
        return List.of("generate_readme", "api_documentation", "release_notes", "technical_documentation", "chat");
    }
    @Override public List<String> limitations() {
        return List.of("Does not invent product claims absent from context");
    }
    @Override public List<String> toolIds() {
        return List.of("core.tool.project_snapshot");
    }
}
