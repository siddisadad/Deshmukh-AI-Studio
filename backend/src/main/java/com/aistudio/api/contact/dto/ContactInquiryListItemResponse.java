package com.aistudio.api.contact.dto;

import java.time.Instant;
import java.util.UUID;

public record ContactInquiryListItemResponse(
        UUID id,
        String name,
        String email,
        String topic,
        String message,
        String sourceIp,
        Instant createdAt,
        Instant readAt
) {
}
