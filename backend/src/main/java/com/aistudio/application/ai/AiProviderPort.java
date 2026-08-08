package com.aistudio.application.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AiProviderPort {
    AiGenerationResult generate(AiGenerationRequest request);

    String providerId();

    /**
     * Streams response text deltas and returns the final generation result.
     * OpenAI and Anthropic adapters call provider-native SSE APIs with usage metadata.
     * Mock falls back to chunked {@link #generate(AiGenerationRequest)} output.
     */
    default AiGenerationResult stream(AiGenerationRequest request, Consumer<String> onDelta) {
        AiGenerationResult result = generate(request);
        chunkText(result.text(), onDelta);
        return result;
    }

    /**
     * Lightweight connectivity probe (models list or minimal completion).
     */
    default boolean probeHealth() {
        return true;
    }

    static void chunkText(String text, Consumer<String> onDelta) {
        if (text == null || text.isBlank()) {
            return;
        }
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(text.length(), i + 24);
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space > i + 8) {
                    end = space + 1;
                }
            }
            onDelta.accept(text.substring(i, end));
            i = end;
            try {
                Thread.sleep(12);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

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
