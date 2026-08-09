package com.aistudio.application.codemetadata;

import java.util.List;

public interface GitMetadataPort {

    String providerId();

    List<GitFileEntry> fetchRepositoryFiles(String repository, String branch);

    default List<GitFileEntry> hydrateFileContents(String repository, String branch, List<GitFileEntry> files) {
        return files;
    }

    default List<GitFileEntry> fetchFilesByPaths(String repository, String branch, List<String> paths) {
        return List.of();
    }

    default void probeCredential() {
    }

    default void probeRepository(String repository, String branch) {
        fetchRepositoryFiles(repository, branch);
    }
}
