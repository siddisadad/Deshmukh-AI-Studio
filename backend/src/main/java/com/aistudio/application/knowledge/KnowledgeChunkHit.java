package com.aistudio.application.knowledge;

import com.aistudio.domain.knowledge.KnowledgeSourceType;
import java.util.UUID;

public record KnowledgeChunkHit(
        UUID id,
        KnowledgeSourceType sourceType,
        UUID sourceId,
        String title,
        String content,
        double score
) {
}
