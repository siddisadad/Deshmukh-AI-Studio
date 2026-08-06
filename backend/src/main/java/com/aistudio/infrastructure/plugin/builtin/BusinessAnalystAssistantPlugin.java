package com.aistudio.infrastructure.plugin.builtin;

import com.aistudio.application.plugin.spi.AssistantPlugin;
import com.aistudio.domain.ai.AssistantRole;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BusinessAnalystAssistantPlugin implements AssistantPlugin {
    @Override public String id() { return "core.assistant.business_analyst"; }
    @Override public String name() { return "Business Analyst"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() { return "Clarifies requirements, stories, and acceptance criteria."; }
    @Override public boolean builtin() { return true; }
    @Override public AssistantRole role() { return AssistantRole.BUSINESS_ANALYST; }
    @Override public String promptKey() { return "business_analyst"; }
    @Override public List<String> capabilities() {
        return List.of("improve_requirements", "user_stories", "acceptance_criteria", "chat");
    }
    @Override public List<String> limitations() {
        return List.of("Does not write production code");
    }
    @Override public List<String> toolIds() {
        return List.of("core.tool.project_snapshot");
    }
}
