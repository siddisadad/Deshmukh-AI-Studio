package com.aistudio.infrastructure.knowledge;

import com.aistudio.application.knowledge.EmbeddingPort;
import com.aistudio.infrastructure.config.AiProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic local embedder for CI/dev. Uses hashed token bags so similar
 * text shares overlapping dimensions without an external API.
 */
@Component
@ConditionalOnProperty(name = "aistudio.ai.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingProvider implements EmbeddingPort {

    public static final int DIMENSIONS = 384;

    private final int batchSize;

    public MockEmbeddingProvider(AiProperties properties) {
        this.batchSize = properties.embedding() == null || properties.embedding().batchSize() <= 0
                ? 64
                : properties.embedding().batchSize();
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            vector[0] = 1f;
            return normalize(vector);
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+");
        for (String token : tokens) {
            if (token.isBlank() || token.length() < 2) {
                continue;
            }
            int h1 = Math.floorMod(hash(token), DIMENSIONS);
            int h2 = Math.floorMod(hash(token + "#2"), DIMENSIONS);
            vector[h1] += 1.0f;
            vector[h2] += 0.5f;
        }
        // lightly boost longer informative tokens
        for (String token : tokens) {
            if (token.length() >= 6) {
                int h = Math.floorMod(hash("long:" + token), DIMENSIONS);
                vector[h] += 0.25f;
            }
        }
        return normalize(vector);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(texts.size(), start + batchSize);
            for (int i = start; i < end; i++) {
                out.add(embed(texts.get(i)));
            }
        }
        return out;
    }

    @Override
    public String providerId() {
        return "mock";
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private static int hash(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int h = 0x811c9dc5;
        for (byte b : bytes) {
            h ^= b;
            h *= 0x01000193;
        }
        return h;
    }

    private static float[] normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        if (sum == 0) {
            vector[0] = 1f;
            return vector;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
        return vector;
    }
}
