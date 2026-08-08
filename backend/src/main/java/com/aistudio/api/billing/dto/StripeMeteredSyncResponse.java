package com.aistudio.api.billing.dto;

import java.util.List;

public record StripeMeteredSyncResponse(
        int processed,
        int synced,
        int skipped,
        int failed,
        List<String> messages
) {
}
