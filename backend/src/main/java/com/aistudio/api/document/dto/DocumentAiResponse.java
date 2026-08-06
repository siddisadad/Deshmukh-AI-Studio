package com.aistudio.api.document.dto;

public record DocumentAiResponse(
        DocumentResponse document,
        String assistantRole,
        String provider,
        String model,
        String generatedText
) {
}
