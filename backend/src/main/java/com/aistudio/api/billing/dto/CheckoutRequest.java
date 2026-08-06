package com.aistudio.api.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank String planCode,
        String successUrl,
        String cancelUrl
) {
}
