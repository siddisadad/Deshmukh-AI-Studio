package com.aistudio.application.codemetadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GitPathIgnoreMatcherTest {

    @Test
    void ignoresPathsMatchingAntStylePatterns() {
        List<String> patterns = GitPathIgnoreMatcher.normalizePatterns(List.of(
                "README.md",
                "**/node_modules/**",
                "*.min.js"
        ));
        assertThat(GitPathIgnoreMatcher.isIgnored("README.md", patterns)).isTrue();
        assertThat(GitPathIgnoreMatcher.isIgnored("src/foo/node_modules/pkg/index.js", patterns)).isTrue();
        assertThat(GitPathIgnoreMatcher.isIgnored("assets/app.min.js", patterns)).isTrue();
        assertThat(GitPathIgnoreMatcher.isIgnored("src/main/App.java", patterns)).isFalse();
    }

    @Test
    void filterFilesRemovesIgnoredEntries() {
        List<String> patterns = GitPathIgnoreMatcher.normalizePatterns(List.of("README.md"));
        List<GitFileEntry> filtered = GitPathIgnoreMatcher.filterFiles(
                List.of(
                        new GitFileEntry("README.md", "markdown", "readme", 10),
                        new GitFileEntry("src/App.java", "java", "class App", 20)
                ),
                patterns
        );
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).path()).isEqualTo("src/App.java");
    }
}
