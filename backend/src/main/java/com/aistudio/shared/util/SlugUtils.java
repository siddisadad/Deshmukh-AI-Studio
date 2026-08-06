package com.aistudio.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-)|(-$)", "");
        if (normalized.isBlank()) {
            return "workspace-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
}
