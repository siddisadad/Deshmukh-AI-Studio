package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.domain.common.AiProviderException;
import com.aistudio.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "aistudio.ai.provider", havingValue = "anthropic")
public class AnthropicProvider implements AiProviderPort {

    private final RestClient client;
    private final String model;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(AiProperties properties, ObjectMapper objectMapper) {
        String apiKey = properties.anthropic() == null ? null : properties.anthropic().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY / aistudio.ai.anthropic.api-key is required when provider=anthropic");
        }
        String baseUrl = properties.anthropic().baseUrl() == null || properties.anthropic().baseUrl().isBlank()
                ? "https://api.anthropic.com"
                : properties.anthropic().baseUrl();
        this.model = properties.anthropic().model() == null || properties.anthropic().model().isBlank()
                ? "claude-sonnet-4-20250514"
                : properties.anthropic().model();
        this.objectMapper = objectMapper;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", request.maxOutputTokens() == null ? 2000 : request.maxOutputTokens());
            if (request.temperature() != null) {
                body.put("temperature", request.temperature());
            }
            body.put("system", request.systemPrompt());
            ArrayNode messages = body.putArray("messages");
            for (AiMessage message : request.messages()) {
                messages.addObject()
                        .put("role", "assistant".equalsIgnoreCase(message.role()) ? "assistant" : "user")
                        .put("content", message.content());
            }

            String response = client.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String text = root.path("content").path(0).path("text").asText("");
            if (text.isBlank()) {
                throw new AiProviderException("Anthropic returned empty content");
            }
            Integer inputTokens = root.path("usage").path("input_tokens").isMissingNode()
                    ? null : root.path("usage").path("input_tokens").asInt();
            Integer outputTokens = root.path("usage").path("output_tokens").isMissingNode()
                    ? null : root.path("usage").path("output_tokens").asInt();
            return new AiGenerationResult(text, model, inputTokens, outputTokens);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("Anthropic request failed", ex);
        }
    }

    @Override
    public String providerId() {
        return "anthropic";
    }
}
