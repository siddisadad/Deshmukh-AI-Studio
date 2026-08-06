package com.aistudio.infrastructure.knowledge;

import com.aistudio.application.knowledge.EmbeddingPort;
import com.aistudio.domain.common.AiProviderException;
import com.aistudio.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "aistudio.ai.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingPort {

    public static final int DIMENSIONS = 384;

    private final RestClient client;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingProvider(AiProperties properties, ObjectMapper objectMapper) {
        String apiKey = properties.openai() == null ? null : properties.openai().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required when embedding.provider=openai");
        }
        String baseUrl = properties.openai().baseUrl() == null || properties.openai().baseUrl().isBlank()
                ? "https://api.openai.com"
                : properties.openai().baseUrl();
        this.model = properties.embedding() == null || properties.embedding().model() == null
                || properties.embedding().model().isBlank()
                ? "text-embedding-3-small"
                : properties.embedding().model();
        this.objectMapper = objectMapper;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text == null ? "" : text)).getFirst();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("dimensions", DIMENSIONS);
            ArrayNode input = body.putArray("input");
            for (String text : texts) {
                input.add(text == null ? "" : text);
            }
            String response = client.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            List<float[]> vectors = new ArrayList<>(texts.size());
            for (int i = 0; i < data.size(); i++) {
                JsonNode emb = data.get(i).path("embedding");
                float[] vector = new float[emb.size()];
                for (int j = 0; j < emb.size(); j++) {
                    vector[j] = (float) emb.get(j).asDouble();
                }
                vectors.add(vector);
            }
            if (vectors.size() != texts.size()) {
                throw new AiProviderException("OpenAI embedding count mismatch");
            }
            return vectors;
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("OpenAI embedding request failed", ex);
        }
    }

    @Override
    public String providerId() {
        return "openai";
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
