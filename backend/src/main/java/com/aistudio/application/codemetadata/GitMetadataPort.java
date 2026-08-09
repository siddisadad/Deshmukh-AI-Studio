package com.aistudio.application.codemetadata;

import java.util.List;

public interface GitMetadataPort {

    List<GitFileEntry> fetchRepositoryFiles(String repository, String branch);
}
