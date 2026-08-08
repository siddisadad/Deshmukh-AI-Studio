package com.aistudio.api.ai.dto;

public record ExportedConversation(byte[] body, String contentType, String filename) {
}
