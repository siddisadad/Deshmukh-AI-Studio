package com.aistudio.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aistudio.application.ai.AiProviderPort;
import com.aistudio.infrastructure.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;

class AnthropicProviderStreamTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
        server.createContext("/v1/messages", exchange -> {
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept == null || !accept.contains("text/event-stream")) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            String sse = ""
                    + "event: message_start\n"
                    + "data: {\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":20,\"output_tokens\":1}}}\n\n"
                    + "event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"delta\":{\"text\":\"Hi\"}}\n\n"
                    + "event: message_delta\n"
                    + "data: {\"type\":\"message_delta\",\"usage\":{\"output_tokens\":4}}\n\n";
            byte[] bytes = sse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void nativeStreamParsesDeltasAndUsage() {
        AnthropicProvider provider = new AnthropicProvider(anthropicProps(baseUrl), new ObjectMapper());
        List<String> deltas = new ArrayList<>();
        AiProviderPort.AiGenerationResult result = provider.stream(
                sampleRequest(),
                deltas::add
        );

        assertThat(deltas).containsExactly("Hi");
        assertThat(result.text()).isEqualTo("Hi");
        assertThat(result.inputTokens()).isEqualTo(20);
        assertThat(result.outputTokens()).isEqualTo(4);
        assertThat(result.model()).isEqualTo("claude-sonnet-4-20250514");
    }

    private static AiProviderPort.AiGenerationRequest sampleRequest() {
        return new AiProviderPort.AiGenerationRequest(
                "system",
                List.of(new AiProviderPort.AiMessage("user", "Hello")),
                0.2,
                100,
                null
        );
    }

    private static AiProperties anthropicProps(String baseUrl) {
        return new AiProperties(
                "anthropic",
                null,
                null,
                null,
                new AiProperties.Anthropic("sk-ant-test", "claude-sonnet-4-20250514", baseUrl),
                null,
                null,
                null,
                null,
                new AiProperties.CircuitBreaker(false, 3, 60),
                null,
                null,
                null,
                null,
                null
        );
    }
}
