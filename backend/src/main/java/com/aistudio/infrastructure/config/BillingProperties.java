package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.billing")
public record BillingProperties(
        String provider,
        String appBaseUrl,
        Stripe stripe
) {
    public record Stripe(
            String apiKey,
            String webhookSecret,
            String proPriceId,
            String teamPriceId
    ) {
        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
