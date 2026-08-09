package com.aistudio.application.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeIndexServiceTest {

    @Test
    void chunkShortTextReturnsSinglePiece() {
        List<String> chunks = KnowledgeIndexService.chunk("hello world", 900, 120);
        assertThat(chunks).containsExactly("hello world");
    }

    @Test
    void chunkSplitsLongTextWithOverlap() {
        String text = "word ".repeat(200).trim();
        List<String> chunks = KnowledgeIndexService.chunk(text, 100, 20);
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.get(0).length()).isLessThanOrEqualTo(100);
    }

    @Test
    void chunkBlankReturnsEmpty() {
        assertThat(KnowledgeIndexService.chunk("   ", 900, 120)).isEmpty();
    }
}
