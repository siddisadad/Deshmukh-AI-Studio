package com.aistudio.infrastructure.codemetadata;

import java.nio.charset.StandardCharsets;

public final class GitSnippetUtils {

    private GitSnippetUtils() {
    }

    public static boolean shouldFetchContent(int sizeBytes, int maxContentFetchBytes) {
        if (maxContentFetchBytes <= 0) {
            return false;
        }
        return sizeBytes <= 0 || sizeBytes <= maxContentFetchBytes;
    }

    public static String truncateToUtf8Bytes(String value, int maxBytes) {
        if (value == null || value.isEmpty() || maxBytes <= 0) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        int end = maxBytes;
        while (end > 0 && (bytes[end - 1] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
