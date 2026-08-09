package com.aistudio.api.billing.dto;

public record StripeDunningRunResponse(
        int processed,
        int notified,
        int skipped,
        java.util.List<String> messages
) {
}
