package com.aistudio.api.requirement.dto;

public record RequirementAiResponse(
        RequirementResponse requirement,
        String assistantRole,
        String provider,
        String model,
        String generatedText
) {
}
