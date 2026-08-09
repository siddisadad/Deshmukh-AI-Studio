package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

    @Test
    void detectsGithubPat() {
        ThreadExportDlpScanResult result = ThreadExportDlpScanner.scan("token ghp_abcdefghijklmnopqrstuvwxyz123456");
        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches().stream().anyMatch(m -> m.category().equals("github_pat"))).isTrue();
    }

    @Test
    void detectsGoogleApiKey() {
        ThreadExportDlpScanResult result = ThreadExportDlpScanner.scan("key AIzaSyD-1234567890abcdefghijklmnopqrs");
        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches().stream().anyMatch(m -> m.category().equals("google_api_key"))).isTrue();
    }

    @Test
    void scanWithCustomPattern() {
        ThreadExportDlpScanResult result = ThreadExportDlpScanner.scanWithCustom(
                "employee id EMP-99999",
                List.of(new ThreadExportDlpCustomPattern("employee_id", "EMP-\\d+", "Employee ID")));
        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches().stream().anyMatch(m -> m.category().equals("employee_id"))).isTrue();
    }
}
