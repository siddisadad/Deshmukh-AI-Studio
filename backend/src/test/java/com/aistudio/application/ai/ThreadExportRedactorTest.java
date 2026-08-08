package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThreadExportRedactorTest {

    @Test
    void noneLeavesContentUnchanged() {
        String raw = "Email me at admin@example.com with sk-test1234567890";
        assertThat(ThreadExportRedactor.redact(raw, ThreadExportRedactionPolicy.NONE)).isEqualTo(raw);
    }

    @Test
    void piiRedactsEmailAndPhone() {
        String raw = "Contact admin@example.com or 555-123-4567";
        String redacted = ThreadExportRedactor.redact(raw, ThreadExportRedactionPolicy.PII);
        assertThat(redacted).doesNotContain("admin@example.com");
        assertThat(redacted).contains("[REDACTED_EMAIL]");
        assertThat(redacted).contains("[REDACTED_PHONE]");
        assertThat(redacted).doesNotContain("sk-");
    }

    @Test
    void secretsRedactsApiKeysAndBearer() {
        String raw = "Use Bearer eyJhbGciOiJIUzI1NiJ9 and key sk-abcdefghijklmnopqrstuvwxyz";
        String redacted = ThreadExportRedactor.redact(raw, ThreadExportRedactionPolicy.SECRETS);
        assertThat(redacted).contains("Bearer [REDACTED_TOKEN]");
        assertThat(redacted).contains("[REDACTED_API_KEY]");
        assertThat(redacted).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
    }

    @Test
    void standardAppliesPiiAndSecrets() {
        String raw = "admin@corp.com token sk-ant-api03-abcdefghijklmnop";
        String redacted = ThreadExportRedactor.redact(raw, ThreadExportRedactionPolicy.STANDARD);
        assertThat(redacted).contains("[REDACTED_EMAIL]");
        assertThat(redacted).contains("[REDACTED_API_KEY]");
    }
}
