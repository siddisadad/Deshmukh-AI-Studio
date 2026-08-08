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

class OpenAiProviderStreamTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
        server.createContext("/v1/chat/completions", exchange -> {
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept == null || !accept.contains("text/event-stream")) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            String sse = ""
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":3}}\n\n"
                    + "data: [DONE]\n\n";
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
        OpenAiProvider provider = new OpenAiProvider(openAiProps(baseUrl), new ObjectMapper());
        List<String> deltas = new ArrayList<>();
        AiProviderPort.AiGenerationResult result = provider.stream(
                sampleRequest(),
                deltas::add
        );

        assertThat(deltas).containsExactly("Hello");
        assertThat(result.text()).isEqualTo("Hello");
        assertThat(result.inputTokens()).isEqualTo(12);
        assertThat(result.outputTokens()).isEqualTo(3);
        assertThat(result.model()).isEqualTo("gpt-4o-mini");
    }

    private static AiProviderPort.AiGenerationRequest sampleRequest() {
        return new AiProviderPort.AiGenerationRequest(
                "system",
                List.of(new AiProviderPort.AiMessage("user", "Hi")),
                0.2,
                100,
                null
        );
    }

    private static AiProperties openAiProps(String baseUrl) {
        return new AiProperties(
                "openai",
                null,
                null,
                new AiProperties.OpenAi("sk-test", "gpt-4o-mini", baseUrl),
                null,
                null,
                null,
                null,
                null
        );
    }
}
