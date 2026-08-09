package com.aistudio.application.codemetadata;

import java.util.List;

public interface GitMetadataPort {

    String providerId();

    List<GitFileEntry> fetchRepositoryFiles(String repository, String branch);

    default List<GitFileEntry> hydrateFileContents(String repository, String branch, List<GitFileEntry> files) {
        return files;
    }
}
