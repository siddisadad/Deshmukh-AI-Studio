package com.aistudio.application.requirement;

import com.aistudio.api.requirement.dto.RequirementAiResponse;
import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.application.ai.ContextBuilder;
import com.aistudio.application.ai.PromptTemplateManager;
import com.aistudio.application.billing.BillingService;
import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import com.aistudio.infrastructure.persistence.repository.RequirementRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementAiService {

    private final RequirementService requirementService;
    private final RequirementRepository requirementRepository;
    private final ContextBuilder contextBuilder;
    private final PromptTemplateManager promptTemplateManager;
    private final AiProviderPort aiProviderPort;
    private final BillingService billingService;

    public RequirementAiService(
            RequirementService requirementService,
            RequirementRepository requirementRepository,
            ContextBuilder contextBuilder,
            PromptTemplateManager promptTemplateManager,
            AiProviderPort aiProviderPort,
            BillingService billingService
    ) {
        this.requirementService = requirementService;
        this.requirementRepository = requirementRepository;
        this.contextBuilder = contextBuilder;
        this.promptTemplateManager = promptTemplateManager;
        this.aiProviderPort = aiProviderPort;
        this.billingService = billingService;
    }

    @Transactional
    public RequirementAiResponse improve(java.util.UUID requirementId, java.util.UUID userId, String instructions) {
        return runAction(requirementId, userId, "ba_improve", instructions, (entity, text) -> entity.setImprovedDescription(text));
    }

    @Transactional
    public RequirementAiResponse userStories(java.util.UUID requirementId, java.util.UUID userId, String instructions) {
        return runAction(requirementId, userId, "ba_user_stories", instructions, (entity, text) -> entity.setUserStories(text));
    }

    @Transactional
    public RequirementAiResponse acceptanceCriteria(java.util.UUID requirementId, java.util.UUID userId, String instructions) {
        return runAction(requirementId, userId, "ba_acceptance_criteria", instructions, (entity, text) -> entity.setAcceptanceCriteria(text));
    }

    private RequirementAiResponse runAction(
            java.util.UUID requirementId,
            java.util.UUID userId,
            String actionKey,
            String instructions,
            FieldWriter writer
    ) {
        RequirementEntity entity = requirementService.requireEditable(requirementId, userId);
        billingService.requireAndConsumeAiActionForProject(entity.getProjectId());
        String context = contextBuilder.buildForProject(
                entity.getProjectId(),
                entity.getTitle() + " " + nullToEmpty(entity.getDescription()) + " " + nullToEmpty(instructions)
        );
        String system = promptTemplateManager.systemPrompt("business_analyst");
        String userPrompt = promptTemplateManager.actionPrompt(actionKey, Map.of(
                "project_context", context,
                "title", entity.getTitle(),
                "description", nullToEmpty(entity.getDescription()),
                "improved_description", nullToEmpty(entity.getImprovedDescription()),
                "user_stories", nullToEmpty(entity.getUserStories()),
                "instructions", nullToEmpty(instructions)
        ));

        AiProviderPort.AiGenerationResult result = aiProviderPort.generate(new AiProviderPort.AiGenerationRequest(
                system,
                List.of(new AiProviderPort.AiMessage("user", userPrompt)),
                0.2,
                2000,
                Map.of(
                        "action", actionKey,
                        "promptVersion", promptTemplateManager.actionPromptVersion(actionKey),
                        "systemPromptVersion", promptTemplateManager.systemPromptVersion("business_analyst")
                )
        ));

        writer.write(entity, result.text());
        requirementRepository.save(entity);

        return new RequirementAiResponse(
                RequirementService.toResponse(entity),
                "BUSINESS_ANALYST",
                aiProviderPort.providerId(),
                result.model(),
                result.text()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface FieldWriter {
        void write(RequirementEntity entity, String text);
    }
}
