package com.aistudio.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeEmbeddingMetrics {

    private final Counter textsEmbedded;
    private final Counter embedBatches;

    public KnowledgeEmbeddingMetrics(MeterRegistry meterRegistry) {
        this.textsEmbedded = Counter.builder("aistudio.knowledge.embeddings.texts")
                .description("Knowledge chunk texts embedded")
                .register(meterRegistry);
        this.embedBatches = Counter.builder("aistudio.knowledge.embeddings.batches")
                .description("Knowledge embedding batch API calls")
                .register(meterRegistry);
    }

    public void recordEmbeddings(int count) {
        if (count <= 0) {
            return;
        }
        textsEmbedded.increment(count);
        embedBatches.increment();
    }
}
