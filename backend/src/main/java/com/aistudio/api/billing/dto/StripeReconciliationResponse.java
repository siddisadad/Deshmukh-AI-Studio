package com.aistudio.api.billing.dto;

import java.time.Instant;

public record StripeReconciliationResponse(
        int processed,
        int matched,
        int mismatched,
        long totalInternalCents,
        long totalStripeCents,
        long toleranceCents,
        Instant runAt,
        java.util.List<String> messages
) {
}
