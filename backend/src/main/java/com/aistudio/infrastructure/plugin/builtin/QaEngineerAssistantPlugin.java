package com.aistudio.infrastructure.plugin.builtin;

import com.aistudio.application.plugin.spi.AssistantPlugin;
import com.aistudio.domain.ai.AssistantRole;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QaEngineerAssistantPlugin implements AssistantPlugin {
    @Override public String id() { return "core.assistant.qa_engineer"; }
    @Override public String name() { return "QA Engineer"; }
    @Override public String version() { return "1.0.0"; }
    @Override public String description() { return "Test cases, API scenarios, and regression checklists."; }
    @Override public boolean builtin() { return true; }
    @Override public AssistantRole role() { return AssistantRole.QA_ENGINEER; }
    @Override public String promptKey() { return "qa_engineer"; }
    @Override public List<String> capabilities() {
        return List.of("generate_test_cases", "api_test_scenarios", "bug_report", "regression_checklist", "chat");
    }
    @Override public List<String> limitations() {
        return List.of("Does not execute tests in MVP");
    }
    @Override public List<String> toolIds() {
        return List.of("core.tool.project_snapshot");
    }
}
