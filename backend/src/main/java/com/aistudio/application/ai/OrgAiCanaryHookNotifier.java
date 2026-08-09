package com.aistudio.application.ai;

import com.aistudio.domain.ai.OrgAiCanaryHookAction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrgAiCanaryHookNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrgAiCanaryHookNotifier.class);

    private final RestClient restClient;

    public OrgAiCanaryHookNotifier(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void notifyIfConfigured(
            String webhookUrl,
            UUID organizationId,
            OrgAiCanaryHookAction action,
            String reason,
            long canarySuccess,
            long canaryFailure,
            long stableSuccess,
            long stableFailure
    ) {
        if (webhookUrl == null || webhookUrl.isBlank() || action == OrgAiCanaryHookAction.NONE) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "org_ai_canary_hook");
        payload.put("organizationId", organizationId);
        payload.put("action", action.name());
        payload.put("reason", reason);
        payload.put("metrics", Map.of(
                "canarySuccessCount", canarySuccess,
                "canaryFailureCount", canaryFailure,
                "stableSuccessCount", stableSuccess,
                "stableFailureCount", stableFailure
        ));
        try {
            restClient.post()
                    .uri(webhookUrl.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to deliver canary hook webhook for orgId={}: {}", organizationId, ex.getMessage());
        }
    }
}
