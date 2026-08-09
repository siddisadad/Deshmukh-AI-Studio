package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.billing")
public record BillingProperties(
        String provider,
        String appBaseUrl,
        String usageSyncToken,
        boolean dunningEnabled,
        int dunningIntervalHours,
        int dunningMaxStage,
        boolean reconciliationEnabled,
        long reconciliationToleranceCents,
        Stripe stripe
) {
    public record Stripe(
            String apiKey,
            String webhookSecret,
            String proPriceId,
            String teamPriceId,
            String proSeatMeteredPriceId,
            String teamSeatMeteredPriceId,
            String proAiOverageMeteredPriceId,
            String teamAiOverageMeteredPriceId
    ) {
        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
