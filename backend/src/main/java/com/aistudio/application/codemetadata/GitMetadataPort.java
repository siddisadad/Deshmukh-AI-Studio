package com.aistudio.application.codemetadata;

import java.util.List;

public interface GitMetadataPort {

    String providerId();

    List<GitFileEntry> fetchRepositoryFiles(String repository, String branch);
}
