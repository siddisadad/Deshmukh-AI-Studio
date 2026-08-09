package com.aistudio.infrastructure.codemetadata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitSnippetUtilsTest {

    @Test
    void truncatesUtf8BytesWithoutSplittingCodePoint() {
        String value = "a".repeat(10);
        assertThat(GitSnippetUtils.truncateToUtf8Bytes(value, 5)).hasSize(5);
    }

    @Test
    void shouldFetchContentWhenSizeUnknownOrWithinLimit() {
        assertThat(GitSnippetUtils.shouldFetchContent(0, 1000)).isTrue();
        assertThat(GitSnippetUtils.shouldFetchContent(500, 1000)).isTrue();
        assertThat(GitSnippetUtils.shouldFetchContent(1001, 1000)).isFalse();
    }
}
