package com.aistudio.infrastructure.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class KnowledgeEmbeddingMetricsTest {

    @Test
    void recordsEmbeddingCounts() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KnowledgeEmbeddingMetrics metrics = new KnowledgeEmbeddingMetrics(registry);
        metrics.recordEmbeddings(12);
        metrics.recordEmbeddings(4);
        assertThat(registry.get("aistudio.knowledge.embeddings.texts").counter().count()).isEqualTo(16);
        assertThat(registry.get("aistudio.knowledge.embeddings.batches").counter().count()).isEqualTo(2);
    }
}
