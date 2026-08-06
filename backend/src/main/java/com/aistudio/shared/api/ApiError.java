package com.aistudio.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId,
        List<FieldError> details
) {
    public record FieldError(String field, String message) {
    }
}
