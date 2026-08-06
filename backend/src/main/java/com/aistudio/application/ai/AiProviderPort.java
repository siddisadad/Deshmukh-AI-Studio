package com.aistudio.application.ai;

import java.util.List;
import java.util.Map;

public interface AiProviderPort {
    AiGenerationResult generate(AiGenerationRequest request);

    String providerId();

    record AiMessage(String role, String content) {
    }

    record AiGenerationRequest(
            String systemPrompt,
            List<AiMessage> messages,
            Double temperature,
            Integer maxOutputTokens,
            Map<String, String> metadata
    ) {
    }

    record AiGenerationResult(
            String text,
            String model,
            Integer inputTokens,
            Integer outputTokens
    ) {
    }
}
