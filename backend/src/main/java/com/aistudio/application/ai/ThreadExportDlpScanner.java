package com.aistudio.application.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Scans export bodies for high-risk patterns that may remain after redaction.
 */
public final class ThreadExportDlpScanner {

    private static final Pattern US_SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PRIVATE_KEY_PEM =
            Pattern.compile("-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----");
    private static final Pattern INTERNAL_HOSTNAME =
            Pattern.compile("\\b[a-z0-9][-a-z0-9]{0,62}\\.(?:internal|corp|local)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWS_SECRET_KEY = Pattern.compile("\\b[A-Za-z0-9/+=]{40}\\b");
    private static final Pattern CREDIT_CARD =
            Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern GITHUB_PAT = Pattern.compile("\\bghp_[A-Za-z0-9]{20,}\\b");
    private static final Pattern GOOGLE_API_KEY = Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b");
    private static final Pattern SLACK_TOKEN = Pattern.compile("\\bxox[baprs]-[0-9A-Za-z-]{10,}\\b");

    private ThreadExportDlpScanner() {
    }

    public static ThreadExportDlpScanResult scan(String content) {
        return scanWithCustom(content, List.of());
    }

    public static ThreadExportDlpScanResult scanWithCustom(
            String content,
            List<ThreadExportDlpCustomPattern> customPatterns
    ) {
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
        if (CREDIT_CARD.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("credit_card", "Credit card number pattern"));
        }
        if (GITHUB_PAT.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("github_pat", "GitHub personal access token"));
        }
        if (GOOGLE_API_KEY.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("google_api_key", "Google API key pattern"));
        }
        if (SLACK_TOKEN.matcher(content).find()) {
            matches.add(new ThreadExportDlpMatch("slack_token", "Slack token pattern"));
        }
        Set<String> seenCustom = new LinkedHashSet<>();
        for (ThreadExportDlpCustomPattern custom : customPatterns) {
            if (custom == null || custom.category() == null || custom.pattern() == null) {
                continue;
            }
            String category = custom.category().trim();
            if (category.isEmpty() || seenCustom.contains(category)) {
                continue;
            }
            try {
                if (Pattern.compile(custom.pattern()).matcher(content).find()) {
                    String description = custom.description() == null || custom.description().isBlank()
                            ? "Custom pattern: " + custom.pattern()
                            : custom.description().trim();
                    matches.add(new ThreadExportDlpMatch(category, description));
                    seenCustom.add(category);
                }
            } catch (PatternSyntaxException ignored) {
                // skip invalid org patterns
            }
        }
        return new ThreadExportDlpScanResult(matches);
    }
}
