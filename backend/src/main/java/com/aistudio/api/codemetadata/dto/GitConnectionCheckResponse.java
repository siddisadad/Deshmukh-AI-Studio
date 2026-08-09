package com.aistudio.api.codemetadata.dto;

public record GitConnectionCheckResponse(
        String name,
        String status,
        String message
) {
}
