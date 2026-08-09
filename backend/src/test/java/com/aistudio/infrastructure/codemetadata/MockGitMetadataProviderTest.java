package com.aistudio.infrastructure.codemetadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.aistudio.application.codemetadata.GitFileEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockGitMetadataProviderTest {

    @Test
    void filtersCodeLikePathsFromTree() {
        List<MockGitMetadataProvider.GitTreeNode> nodes = List.of(
                new MockGitMetadataProvider.GitTreeNode("src/App.java", "blob", 1200),
                new MockGitMetadataProvider.GitTreeNode("dist/bundle.js", "blob", 9000),
                new MockGitMetadataProvider.GitTreeNode("README.md", "blob", 200),
                new MockGitMetadataProvider.GitTreeNode("assets/logo.png", "blob", 4000)
        );
        List<GitFileEntry> files = MockGitMetadataProvider.filterCodePaths(nodes, 10);
        assertThat(files).extracting(GitFileEntry::path)
                .containsExactly("src/App.java", "dist/bundle.js", "README.md");
    }
}
