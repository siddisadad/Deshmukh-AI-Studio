package com.aistudio.application.ai;

import java.util.regex.Pattern;

/**
 * Applies regex-based redaction to exported thread message content.
 */
public final class ThreadExportRedactor {

    private static final Pattern EMAIL =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE =
            Pattern.compile("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}\\b");
    private static final Pattern CREDIT_CARD =
            Pattern.compile("\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b");
    private static final Pattern BEARER =
            Pattern.compile("Bearer\\s+[A-Za-z0-9._\\-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPENAI_KEY = Pattern.compile("\\bsk-[A-Za-z0-9]{10,}\\b");
    private static final Pattern ANTHROPIC_KEY = Pattern.compile("\\bsk-ant-[A-Za-z0-9\\-_]{10,}\\b");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
    private static final Pattern JSON_SECRET_FIELD =
            Pattern.compile("(\"(?:password|secret|api[_-]?key|token)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENV_SECRET =
            Pattern.compile(
                    "(?i)(OPENAI_API_KEY|ANTHROPIC_API_KEY|JWT_SECRET|STRIPE_[A-Z_]+|AWS_SECRET_ACCESS_KEY)\\s*[=:]\\s*\\S+");

    private ThreadExportRedactor() {
    }

    public static String redact(String text, ThreadExportRedactionPolicy policy) {
        if (text == null || text.isEmpty() || policy == ThreadExportRedactionPolicy.NONE) {
            return text;
        }
        String result = text;
        if (policy == ThreadExportRedactionPolicy.PII || policy == ThreadExportRedactionPolicy.STANDARD) {
            result = EMAIL.matcher(result).replaceAll("[REDACTED_EMAIL]");
            result = PHONE.matcher(result).replaceAll("[REDACTED_PHONE]");
            result = CREDIT_CARD.matcher(result).replaceAll("[REDACTED_CARD]");
        }
        if (policy == ThreadExportRedactionPolicy.SECRETS || policy == ThreadExportRedactionPolicy.STANDARD) {
            result = BEARER.matcher(result).replaceAll("Bearer [REDACTED_TOKEN]");
            result = OPENAI_KEY.matcher(result).replaceAll("[REDACTED_API_KEY]");
            result = ANTHROPIC_KEY.matcher(result).replaceAll("[REDACTED_API_KEY]");
            result = AWS_ACCESS_KEY.matcher(result).replaceAll("[REDACTED_AWS_KEY]");
            result = JSON_SECRET_FIELD.matcher(result).replaceAll("$1\"[REDACTED]\"");
            result = ENV_SECRET.matcher(result).replaceAll("$1=[REDACTED]");
        }
        return result;
    }
}
