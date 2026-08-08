package com.aistudio.api.billing.dto;

import java.time.Instant;

public record InvoiceResponse(
        String id,
        String number,
        String status,
        long amountDueCents,
        String currency,
        Instant createdAt,
        String hostedInvoiceUrl,
        String invoicePdfUrl
) {
}
