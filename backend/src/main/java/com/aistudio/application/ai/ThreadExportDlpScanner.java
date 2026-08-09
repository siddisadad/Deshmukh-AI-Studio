package com.aistudio.application.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scans export bodies for high-risk patterns that may remain after redaction.
 */
public final class ThreadExportDlpScanner {

    private static final Pattern US_SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PRIVATE_KEY_PEM =
            Pattern.compile("-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----");
    private static final Pattern INTERNAL_HOSTNAME =
            Pattern.compile("\\b[a-z0-9][-a-z0-9]{0,62}\\.(?:internal|corp|local)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWS_SECRET_KEY =
            Pattern.compile("\\b[A-Za-z0-9/+=]{40}\\b");

    private ThreadExportDlpScanner() {
    }

    public static ThreadExportDlpScanResult scan(String content) {
        if (content == null || content.isEmpty()) {
            return new ThreadExportDlpScanResult(List.of());
        }
        List<ThreadExportDlpMatch> matches = new ArrayList<>();
        if (US_SSN.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("ssn", "US Social Security number pattern"));
        }
        if (PRIVATE_KEY_PEM.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("private_key", "PEM private key block"));
        }
        if (INTERNAL_HOSTNAME.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("internal_hostname", "Internal/corp hostname"));
        }
        if (content.contains("AWS_SECRET_ACCESS_KEY") && AWS_SECRET_KEY.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("aws_secret_key", "AWS secret access key pattern"));
        }
        return new ThreadExportDlpScanResult(matches);
    }
}
