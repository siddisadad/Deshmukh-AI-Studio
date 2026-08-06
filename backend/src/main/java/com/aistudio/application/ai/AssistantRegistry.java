package com.aistudio.application.ai;

import com.aistudio.domain.ai.AssistantRole;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AssistantRegistry {

    public record AssistantDefinition(
            AssistantRole role,
            String name,
            String promptKey,
            List<String> capabilities,
            List<String> limitations
    ) {
    }

    private final List<AssistantDefinition> assistants = List.of(
            new AssistantDefinition(
                    AssistantRole.BUSINESS_ANALYST,
                    "Business Analyst",
                    "business_analyst",
                    List.of("improve_requirements", "user_stories", "acceptance_criteria", "chat"),
                    List.of("Does not write production code")
            ),
            new AssistantDefinition(
                    AssistantRole.DEVELOPER,
                    "Developer",
                    "developer",
                    List.of("api_suggestions", "db_suggestions", "code_examples", "code_review", "chat"),
                    List.of("Does not deploy; treats pasted code as untrusted")
            ),
            new AssistantDefinition(
                    AssistantRole.QA_ENGINEER,
                    "QA Engineer",
                    "qa_engineer",
                    List.of("generate_test_cases", "api_test_scenarios", "bug_report", "regression_checklist", "chat"),
                    List.of("Does not execute tests in MVP")
            ),
            new AssistantDefinition(
                    AssistantRole.DOCUMENTATION_WRITER,
                    "Documentation Writer",
                    "documentation_writer",
                    List.of("generate_readme", "api_documentation", "release_notes", "technical_documentation", "chat"),
                    List.of("Does not invent product claims absent from context")
            )
    );

    public List<AssistantDefinition> all() {
        return assistants;
    }

    public AssistantDefinition require(AssistantRole role) {
        return assistants.stream()
                .filter(a -> a.role() == role)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant: " + role));
    }

    public AssistantRole parseRole(String value) {
        return Arrays.stream(AssistantRole.values())
                .filter(r -> r.name().equalsIgnoreCase(value) || r.name().replace("_", "").equalsIgnoreCase(value.replace("-", "").replace("_", "")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant role: " + value));
    }
}
