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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "aistudio.ai.provider", havingValue = "openai")
public class OpenAiProvider implements AiProviderPort {

    private final RestClient client;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiProvider(AiProperties properties, ObjectMapper objectMapper) {
        String apiKey = properties.openai() == null ? null : properties.openai().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY / aistudio.ai.openai.api-key is required when provider=openai");
        }
        String baseUrl = properties.openai().baseUrl() == null || properties.openai().baseUrl().isBlank()
                ? "https://api.openai.com"
                : properties.openai().baseUrl();
        this.model = properties.openai().model() == null || properties.openai().model().isBlank()
                ? "gpt-4o-mini"
                : properties.openai().model();
        this.objectMapper = objectMapper;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request) {
        try {
            ObjectNode body = buildBody(request, false);
            String response = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            if (text.isBlank()) {
                throw new AiProviderException("OpenAI returned empty content");
            }
            Integer inputTokens = root.path("usage").path("prompt_tokens").isMissingNode()
                    ? null : root.path("usage").path("prompt_tokens").asInt();
            Integer outputTokens = root.path("usage").path("completion_tokens").isMissingNode()
                    ? null : root.path("usage").path("completion_tokens").asInt();
            return new AiGenerationResult(text, model, inputTokens, outputTokens);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("OpenAI request failed", ex);
        }
    }

    @Override
    public AiGenerationResult stream(AiGenerationRequest request, Consumer<String> onDelta) {
        try {
            ObjectNode body = buildBody(request, true);
            StringBuilder full = new StringBuilder();
            client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body.toString())
                    .exchange((req, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw new AiProviderException("OpenAI stream failed with status " + response.getStatusCode().value());
                        }
                        InputStream stream = response.getBody();
                        if (stream == null) {
                            throw new AiProviderException("OpenAI stream returned empty body");
                        }
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String payload = line.substring(5).trim();
                                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                                    continue;
                                }
                                JsonNode root = objectMapper.readTree(payload);
                                String delta = root.path("choices").path(0).path("delta").path("content").asText(null);
                                if (delta != null && !delta.isEmpty()) {
                                    full.append(delta);
                                    onDelta.accept(delta);
                                }
                            }
                        }
                        return null;
                    });
            if (full.isEmpty()) {
                throw new AiProviderException("OpenAI stream returned empty content");
            }
            return new AiGenerationResult(full.toString(), model, null, null);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("OpenAI stream request failed", ex);
        }
    }

    @Override
    public String providerId() {
        return "openai";
    }

    private ObjectNode buildBody(AiGenerationRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", stream);
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        if (request.maxOutputTokens() != null) {
            body.put("max_tokens", request.maxOutputTokens());
        }
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", request.systemPrompt());
        for (AiMessage message : request.messages()) {
            messages.addObject().put("role", mapRole(message.role())).put("content", message.content());
        }
        return body;
    }

    private static String mapRole(String role) {
        if ("assistant".equalsIgnoreCase(role)) {
            return "assistant";
        }
        return "user";
    }
}
