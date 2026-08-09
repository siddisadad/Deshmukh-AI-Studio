package com.aistudio.application.ai;

/**
 * Provider and model selected for a chat request (assistant-role or org policy).
 */
public record AiModelRoute(String providerId, String model) {
}
