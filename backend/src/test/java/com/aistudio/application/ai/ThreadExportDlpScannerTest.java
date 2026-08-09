package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThreadExportDlpScannerTest {

    @Test
    void detectsSsnPattern() {
        ThreadExportDlpScanResult result = ThreadExportDlpScanner.scan("employee ssn 123-45-6789");
        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches().get(0).category()).isEqualTo("ssn");
    }

    @Test
    void detectsPrivateKeyBlock() {
        String content = "-----BEGIN RSA PRIVATE KEY-----\nMIIE";
        ThreadExportDlpScanResult result = ThreadExportDlpScanner.scan(content);
        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches().stream().anyMatch(m -> m.category().equals("private_key"))).isTrue();
    }

    @Test
    void cleanContentHasNoMatches() {
        ThreadExportDlpScanResult result = ThreadExportDlpScanner.scan("Hello export without sensitive data");
        assertThat(result.hasMatches()).isFalse();
    }
}
