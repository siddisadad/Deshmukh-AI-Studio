package com.aistudio.infrastructure.ai;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.domain.common.AiProviderException;
import com.aistudio.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

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
            ObjectNode body = buildBody(request, false);
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
    public AiGenerationResult stream(AiGenerationRequest request, Consumer<String> onDelta) {
        try {
            ObjectNode body = buildBody(request, true);
            StringBuilder full = new StringBuilder();
            int[] inputTokens = new int[1];
            int[] outputTokens = new int[1];
            inputTokens[0] = -1;
            outputTokens[0] = -1;
            client.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body.toString())
                    .exchange((req, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw new AiProviderException("Anthropic stream failed with status " + response.getStatusCode().value());
                        }
                        InputStream stream = response.getBody();
                        if (stream == null) {
                            throw new AiProviderException("Anthropic stream returned empty body");
                        }
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String payload = line.substring(5).trim();
                                if (payload.isEmpty()) {
                                    continue;
                                }
                                JsonNode root = objectMapper.readTree(payload);
                                String type = root.path("type").asText("");
                                if ("message_start".equals(type)) {
                                    JsonNode usage = root.path("message").path("usage");
                                    if (!usage.path("input_tokens").isMissingNode()) {
                                        inputTokens[0] = usage.path("input_tokens").asInt();
                                    }
                                } else if ("content_block_delta".equals(type)) {
                                    String delta = root.path("delta").path("text").asText(null);
                                    if (delta != null && !delta.isEmpty()) {
                                        full.append(delta);
                                        onDelta.accept(delta);
                                    }
                                } else if ("message_delta".equals(type)) {
                                    JsonNode usage = root.path("usage");
                                    if (!usage.path("output_tokens").isMissingNode()) {
                                        outputTokens[0] = usage.path("output_tokens").asInt();
                                    }
                                }
                            }
                        }
                        return null;
                    });
            if (full.isEmpty()) {
                throw new AiProviderException("Anthropic stream returned empty content");
            }
            return new AiGenerationResult(
                    full.toString(),
                    model,
                    inputTokens[0] >= 0 ? inputTokens[0] : null,
                    outputTokens[0] >= 0 ? outputTokens[0] : null
            );
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("Anthropic stream request failed", ex);
        }
    }

    @Override
    public String providerId() {
        return "anthropic";
    }

    @Override
    public boolean probeHealth() {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 1);
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "user").put("content", "ping");
            client.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private ObjectNode buildBody(AiGenerationRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", stream);
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
        return body;
    }
}
