package com.aistudio.application.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ThreadExportDlpNotifier {

    private static final Logger log = LoggerFactory.getLogger(ThreadExportDlpNotifier.class);

    private final RestClient restClient;

    public ThreadExportDlpNotifier(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void notifyIfConfigured(
            String webhookUrl,
            UUID exportId,
            UUID exportedByUserId,
            UUID projectId,
            UUID conversationId,
            ThreadExportDlpScanResult scanResult
    ) {
        if (webhookUrl == null || webhookUrl.isBlank() || !scanResult.hasMatches()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "chat_export_dlp_match");
        payload.put("exportId", exportId);
        payload.put("exportedByUserId", exportedByUserId);
        payload.put("projectId", projectId);
        if (conversationId != null) {
            payload.put("conversationId", conversationId);
        }
        payload.put("matches", scanResult.matches().stream()
                .map(match -> Map.of("category", match.category(), "description", match.description()))
                .toList());
        try {
            restClient.post()
                    .uri(webhookUrl.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to deliver export DLP webhook for exportId={}: {}", exportId, ex.getMessage());
        }
    }
}
