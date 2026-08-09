package com.aistudio.application.codemetadata;

import java.util.List;

public record GitFileEntry(String path, String language, String snippet, int sizeBytes) {

    public static List<GitFileEntry> fromManifestInputs(List<ManifestFileInput> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .map(f -> new GitFileEntry(
                        f.path(),
                        f.language() == null ? "" : f.language(),
                        f.snippet() == null ? "" : f.snippet(),
                        Math.max(0, f.sizeBytes())
                ))
                .toList();
    }

    public record ManifestFileInput(String path, String language, String snippet, int sizeBytes) {
    }
}
