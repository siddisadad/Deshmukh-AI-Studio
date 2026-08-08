package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import java.util.Locale;
import java.util.Map;
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
        int inputTokens = estimateTokens(request.systemPrompt())
                + request.messages().stream().mapToInt(m -> estimateTokens(m.content())).sum();
        int outputTokens = estimateTokens(text);
        return new AiGenerationResult(text, "mock-1", inputTokens, outputTokens);
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

        String action = metadataAction(request.metadata());
        if (action != null) {
            String routed = routeAction(action, user);
            if (routed != null) {
                return routed;
            }
        }

        String lower = (request.systemPrompt() + "\n" + user).toLowerCase(Locale.ROOT);
        if (lower.contains("summarize") && lower.contains("requirement")) {
            return summarizeRequirements();
        }
        if (lower.contains("acceptance criteria")) {
            return acceptanceCriteria();
        }
        if (lower.contains("user stor")) {
            return userStories();
        }
        if (lower.contains("improv")) {
            return improveRequirement(user);
        }
        if (lower.contains("documentation") || lower.contains("generate markdown documentation") || lower.contains("readme")) {
            return generateDocumentation(user);
        }
        return "Mock AI response grounded in project context:\n\n" + truncate(user, 1000);
    }

    private static String metadataAction(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        String action = metadata.get("action");
        return action == null || action.isBlank() ? null : action;
    }

    private static String routeAction(String action, String user) {
        switch (action) {
            case "ba_acceptance_criteria":
                return acceptanceCriteria();
            case "ba_user_stories":
                return userStories();
            case "ba_improve":
                return improveRequirement(user);
            case "docs_generate":
                return generateDocumentation(user);
            default:
                return null;
        }
    }

    private static String summarizeRequirements() {
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

    private static String acceptanceCriteria() {
        return """
                ## Acceptance criteria
                - Given a valid request, when the action is performed, then the system persists the result.
                - Given invalid input, when the action is performed, then the system returns a validation error.
                - Given unauthorized access, when the resource is requested, then the system returns 404/403.
                """.strip();
    }

    private static String userStories() {
        return """
                ## User stories
                - As a project member, I want clear requirements so that implementation matches intent.
                - As a QA engineer, I want acceptance criteria so that I can verify behavior.
                - As a product owner, I want prioritized stories so that the team delivers value first.
                """.strip();
    }

    private static String improveRequirement(String user) {
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

    private static String generateDocumentation(String user) {
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "\n…[truncated]";
    }

    /** Rough token estimate for mock metering (chars / 4). */
    private static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }
}
