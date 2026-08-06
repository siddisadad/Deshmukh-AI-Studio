package com.aistudio.infrastructure.plugin.builtin;

import com.aistudio.application.plugin.spi.AssistantPlugin;
import com.aistudio.domain.ai.AssistantRole;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeveloperAssistantPlugin implements AssistantPlugin {
    @Override public String id() { return "core.assistant.developer"; }
    @Override public String name() { return "Developer"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() { return "Technical design, APIs, and code guidance."; }
    @Override public boolean builtin() { return true; }
    @Override public AssistantRole role() { return AssistantRole.DEVELOPER; }
    @Override public String promptKey() { return "developer"; }
    @Override public List<String> capabilities() {
        return List.of("api_suggestions", "db_suggestions", "code_examples", "code_review", "chat");
    }
    @Override public List<String> limitations() {
        return List.of("Does not deploy; treats pasted code as untrusted");
    }
    @Override public List<String> toolIds() {
        return List.of("core.tool.project_snapshot");
    }
}
