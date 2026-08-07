package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProviderPort {

    @Override
    public AiGenerationResult generate(AiGenerationRequest request) {
        String text = buildText(request);
        return new AiGenerationResult(text, "mock-1", null, null);
    }

    @Override
    public AiGenerationResult stream(AiGenerationRequest request, Consumer<String> onDelta) {
        AiGenerationResult result = generate(request);
        AiProviderPort.chunkText(result.text(), onDelta);
        return result;
    }

    @Override
    public String providerId() {
        return "mock";
    }

    private static String buildText(AiGenerationRequest request) {
        String user = request.messages().stream()
                .map(AiMessage::content)
                .collect(Collectors.joining("\n"));
        String lower = (request.systemPrompt() + "\n" + user).toLowerCase(Locale.ROOT);
        if (lower.contains("summarize") && lower.contains("requirement")) {
            return """
                    ## Open requirements summary

                    Based on the shared project context, prioritize requirements that block implementation or testing.

                    ### Highlights
                    - Capture actor, trigger, and expected outcome for each open requirement.
                    - Link tasks to requirements so Kanban progress reflects delivery.
                    - Add acceptance criteria before moving stories to implementation.

                    ### Suggested next steps
                    1. Review the highest-priority requirement and confirm acceptance criteria.
                    2. Create or update tasks for the next sprint slice.
                    3. Ask clarifying questions where context is incomplete.
                    """.strip();
        }
        if (lower.contains("acceptance criteria")) {
            return """
                    ## Acceptance Criteria
                    - Given a valid request, when the action is performed, then the system persists the result.
                    - Given invalid input, when the action is performed, then the system returns a validation error.
                    - Given unauthorized access, when the resource is requested, then the system returns 404/403.
                    """.strip();
        }
        if (lower.contains("user stor")) {
            return """
                    ## User Stories
                    - As a project member, I want clear requirements so that implementation matches intent.
                    - As a QA engineer, I want acceptance criteria so that I can verify behavior.
                    - As a product owner, I want prioritized stories so that the team delivers value first.
                    """.strip();
        }
        if (lower.contains("improv")) {
            return """
                    ## Improved requirement
                    Clarify the actor, trigger, expected outcome, and non-functional constraints.
                    Remove ambiguity, state assumptions explicitly, and keep the language testable.

                    ## Assumptions
                    - Assumption: stakeholders agree on the priority stated in context.

                    ## Open questions
                    1. Who is the primary actor for this flow?
                    2. What error states must be supported?
                    """.strip() + "\n\n" + truncate(user, 400);
        }
        if (lower.contains("documentation") || lower.contains("generate markdown documentation") || lower.contains("readme")) {
            return """
                    # Project Documentation

                    ## Overview
                    This document was generated from the shared project context.

                    ## Getting started
                    1. Review requirements and tasks in AI Studio.
                    2. Confirm acceptance criteria before implementation.
                    3. Keep this document updated as the project evolves.

                    ## Notes
                    """.strip() + "\n\n" + truncate(user, 600);
        }
        return "Mock AI response grounded in project context:\n\n" + truncate(user, 1000);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "\n…[truncated]";
    }
}
