package com.aistudio.application.export;

import com.aistudio.domain.export.OrgDlpConnectorType;
import com.aistudio.infrastructure.persistence.entity.OrgDlpConnectorEntity;
import com.aistudio.infrastructure.persistence.entity.ThreadExportDlpEventEntity;
import com.aistudio.infrastructure.persistence.repository.OrgDlpConnectorRepository;
import com.aistudio.infrastructure.persistence.repository.ThreadExportDlpEventRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class SiemExportService {

    private static final Logger log = LoggerFactory.getLogger(SiemExportService.class);

    private final ThreadExportDlpEventRepository eventRepository;
    private final OrgDlpConnectorRepository connectorRepository;
    private final RestClient restClient;

    public SiemExportService(
            ThreadExportDlpEventRepository eventRepository,
            OrgDlpConnectorRepository connectorRepository,
            RestClient.Builder restClientBuilder
    ) {
        this.eventRepository = eventRepository;
        this.connectorRepository = connectorRepository;
        this.restClient = restClientBuilder.build();
    }

    @Transactional
    public SiemExportRunResult exportPendingEvents() {
        List<ThreadExportDlpEventEntity> pending = eventRepository.findPendingSiemExport();
        if (pending.isEmpty()) {
            return new SiemExportRunResult(0, 0, 0, List.of("No pending DLP events"));
        }
        List<OrgDlpConnectorEntity> siemConnectors =
                connectorRepository.findByConnectorTypeAndEnabledTrue(OrgDlpConnectorType.SIEM);
        if (siemConnectors.isEmpty()) {
            return new SiemExportRunResult(0, 0, pending.size(), List.of("No enabled SIEM connectors"));
        }
        int exported = 0;
        int failed = 0;
        List<String> messages = new ArrayList<>();
        for (ThreadExportDlpEventEntity event : pending) {
            List<OrgDlpConnectorEntity> orgConnectors = siemConnectors.stream()
                    .filter(c -> c.getOrganizationId().equals(event.getOrganizationId()))
                    .toList();
            if (orgConnectors.isEmpty()) {
                failed++;
                messages.add(event.getId() + ": skip — no SIEM connector for org");
                continue;
            }
            boolean delivered = false;
            for (OrgDlpConnectorEntity connector : orgConnectors) {
                if (deliverToSiem(connector, event)) {
                    delivered = true;
                }
            }
            if (delivered) {
                event.setSiemExportedAt(java.time.Instant.now());
                eventRepository.save(event);
                exported++;
                messages.add(event.getId() + ": exported to SIEM");
            } else {
                failed++;
                messages.add(event.getId() + ": fail — SIEM delivery failed");
            }
        }
        return new SiemExportRunResult(pending.size(), exported, failed, messages);
    }

    private boolean deliverToSiem(OrgDlpConnectorEntity connector, ThreadExportDlpEventEntity event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "chat_export_dlp_siem");
        payload.put("connectorSlug", connector.getSlug());
        payload.put("eventId", event.getId());
        payload.put("organizationId", event.getOrganizationId());
        payload.put("projectId", event.getProjectId());
        payload.put("conversationId", event.getConversationId());
        payload.put("exportId", event.getExportId());
        payload.put("exportedByUserId", event.getExportedByUserId());
        payload.put("matchCategories", event.getMatchCategories());
        payload.put("blocked", event.isBlocked());
        payload.put("createdAt", event.getCreatedAt());
        try {
            restClient.post()
                    .uri(connector.getWebhookUrl().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("SIEM export failed for event {} connector {}: {}", event.getId(), connector.getSlug(), ex.getMessage());
            return false;
        }
    }

    public record SiemExportRunResult(int processed, int exported, int failed, List<String> messages) {
    }
}
