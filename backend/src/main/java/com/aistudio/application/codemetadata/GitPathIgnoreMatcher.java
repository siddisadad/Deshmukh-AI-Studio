package com.aistudio.application.codemetadata;

import com.aistudio.application.codemetadata.GitFileEntry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.AntPathMatcher;

public final class GitPathIgnoreMatcher {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private GitPathIgnoreMatcher() {
    }

    public static List<String> normalizePatterns(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String pattern : raw) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String normalized = normalizePattern(pattern.trim());
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out;
    }

    public static boolean isIgnored(String path, List<String> patterns) {
        if (path == null || path.isBlank() || patterns == null || patterns.isEmpty()) {
            return false;
        }
        return matchesAny(path, patterns);
    }

    public static boolean isIncluded(String path, List<String> includePatterns) {
        if (includePatterns == null || includePatterns.isEmpty()) {
            return true;
        }
        if (path == null || path.isBlank()) {
            return false;
        }
        return matchesAny(path, includePatterns);
    }

    public static List<GitFileEntry> filterIncludedFiles(List<GitFileEntry> files, List<String> includePatterns) {
        if (files == null || files.isEmpty() || includePatterns == null || includePatterns.isEmpty()) {
            return files == null ? List.of() : files;
        }
        List<GitFileEntry> out = new ArrayList<>();
        for (GitFileEntry file : files) {
            if (isIncluded(file.path(), includePatterns)) {
                out.add(file);
            }
        }
        return out;
    }

    public static List<String> filterIncludedPaths(List<String> paths, List<String> includePatterns) {
        if (paths == null || paths.isEmpty() || includePatterns == null || includePatterns.isEmpty()) {
            return paths == null ? List.of() : paths;
        }
        List<String> out = new ArrayList<>();
        for (String path : paths) {
            if (isIncluded(path, includePatterns)) {
                out.add(path);
            }
        }
        return out;
    }

    private static boolean matchesAny(String path, List<String> patterns) {
        String normalizedPath = normalizePath(path);
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (MATCHER.match(pattern, normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    public static List<GitFileEntry> filterFiles(List<GitFileEntry> files, List<String> patterns) {
        if (files == null || files.isEmpty() || patterns == null || patterns.isEmpty()) {
            return files == null ? List.of() : files;
        }
        List<GitFileEntry> out = new ArrayList<>();
        for (GitFileEntry file : files) {
            if (!isIgnored(file.path(), patterns)) {
                out.add(file);
            }
        }
        return out;
    }

    public static List<String> filterPaths(List<String> paths, List<String> patterns) {
        if (paths == null || paths.isEmpty() || patterns == null || patterns.isEmpty()) {
            return paths == null ? List.of() : paths;
        }
        List<String> out = new ArrayList<>();
        for (String path : paths) {
            if (!isIgnored(path, patterns)) {
                out.add(path);
            }
        }
        return out;
    }

    static String normalizePattern(String pattern) {
        if (pattern.isEmpty()) {
            return "";
        }
        if (!pattern.contains("/")) {
            if (pattern.startsWith("**/")) {
                return pattern;
            }
            return "**/" + pattern;
        }
        return pattern;
    }

    private static String normalizePath(String path) {
        String trimmed = path.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }
}
