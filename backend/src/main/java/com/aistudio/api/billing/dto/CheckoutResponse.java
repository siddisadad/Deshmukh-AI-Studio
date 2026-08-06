package com.aistudio.api.billing.dto;

public record CheckoutResponse(
        String sessionId,
        String checkoutUrl,
        String provider
) {
}
