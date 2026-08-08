package com.aistudio.application.ai;

import com.aistudio.domain.common.DomainException;
import java.util.Locale;

/**
 * Named redaction policies for conversation thread exports.
 */
public enum ThreadExportRedactionPolicy {
    NONE("none"),
    PII("pii"),
    SECRETS("secrets"),
    STANDARD("standard");

    private final String wireValue;

    ThreadExportRedactionPolicy(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static ThreadExportRedactionPolicy fromWireValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "redaction must be none, pii, secrets, or standard");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ThreadExportRedactionPolicy policy : values()) {
            if (policy.wireValue.equals(normalized)) {
                return policy;
            }
        }
        throw new DomainException("VALIDATION_ERROR", "redaction must be none, pii, secrets, or standard");
    }
}
