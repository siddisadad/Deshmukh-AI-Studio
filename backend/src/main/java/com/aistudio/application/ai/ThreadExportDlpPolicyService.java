package com.aistudio.application.ai;

import com.aistudio.api.ai.dto.ExportedConversation;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.export.OrgDlpConnectorType;
import com.aistudio.infrastructure.persistence.entity.OrgDlpConnectorEntity;
import com.aistudio.infrastructure.persistence.entity.ThreadExportDlpEventEntity;
import com.aistudio.infrastructure.persistence.repository.OrgDlpConnectorRepository;
import com.aistudio.infrastructure.persistence.repository.ThreadExportDlpEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThreadExportDlpPolicyService {

    private final boolean exportDlpEnabled;
    private final boolean exportDlpBlockOnMatch;
    private final String exportDlpWebhookUrl;
    private final ThreadExportDlpNotifier exportDlpNotifier;
    private final OrgDlpConnectorRepository connectorRepository;
    private final ThreadExportDlpEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public ThreadExportDlpPolicyService(
            @Value("${aistudio.ai.conversation.export-dlp-enabled:false}") boolean exportDlpEnabled,
            @Value("${aistudio.ai.conversation.export-dlp-block-on-match:true}") boolean exportDlpBlockOnMatch,
            @Value("${aistudio.ai.conversation.export-dlp-webhook-url:}") String exportDlpWebhookUrl,
            ThreadExportDlpNotifier exportDlpNotifier,
            OrgDlpConnectorRepository connectorRepository,
            ThreadExportDlpEventRepository eventRepository,
            ObjectMapper objectMapper
    ) {
        this.exportDlpEnabled = exportDlpEnabled;
        this.exportDlpBlockOnMatch = exportDlpBlockOnMatch;
        this.exportDlpWebhookUrl = exportDlpWebhookUrl;
        this.exportDlpNotifier = exportDlpNotifier;
        this.connectorRepository = connectorRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExportedConversation apply(
            ExportedConversation exported,
            ThreadExportWatermark.Metadata exportMetadata,
            UUID organizationId,
            UUID projectId,
            UUID conversationId
    ) {
        List<OrgDlpConnectorEntity> orgConnectors =
                connectorRepository.findByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(organizationId);
        boolean orgWebhookEnabled = orgConnectors.stream()
                .anyMatch(c -> c.getConnectorType() == OrgDlpConnectorType.WEBHOOK);
        if (!exportDlpEnabled && !orgWebhookEnabled) {
            return exported;
        }
        String text = new String(exported.body(), StandardCharsets.UTF_8);
        List<ThreadExportDlpCustomPattern> customPatterns = collectCustomPatterns(orgConnectors);
        ThreadExportDlpScanResult scanResult = ThreadExportDlpScanner.scanWithCustom(text, customPatterns);
        if (!scanResult.hasMatches()) {
            return exported;
        }
        boolean block = shouldBlock(scanResult, orgConnectors);
        recordEvent(organizationId, projectId, conversationId, exportMetadata, scanResult, block);
        if (exportDlpEnabled) {
            exportDlpNotifier.notifyIfConfigured(
                    exportDlpWebhookUrl,
                    exportMetadata.exportId(),
                    exportMetadata.exportedByUserId(),
                    projectId,
                    conversationId,
                    scanResult);
        }
        for (OrgDlpConnectorEntity connector : orgConnectors) {
            if (connector.getConnectorType() != OrgDlpConnectorType.WEBHOOK) {
                continue;
            }
            exportDlpNotifier.notifyIfConfigured(
                    connector.getWebhookUrl(),
                    exportMetadata.exportId(),
                    exportMetadata.exportedByUserId(),
                    projectId,
                    conversationId,
                    scanResult);
        }
        if (block) {
            String categories = scanResult.matches().stream()
                    .map(ThreadExportDlpMatch::category)
                    .distinct()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("policy");
            throw new DomainException("FORBIDDEN", "Export blocked by DLP policy: " + categories);
        }
        return exported;
    }

    private boolean shouldBlock(ThreadExportDlpScanResult scanResult, List<OrgDlpConnectorEntity> orgConnectors) {
        if (exportDlpEnabled && exportDlpBlockOnMatch && scanResult.hasMatches()) {
            return true;
        }
        return orgConnectors.stream()
                .filter(c -> c.getConnectorType() == OrgDlpConnectorType.WEBHOOK)
                .anyMatch(OrgDlpConnectorEntity::isBlockOnMatch);
    }

    private void recordEvent(
            UUID organizationId,
            UUID projectId,
            UUID conversationId,
            ThreadExportWatermark.Metadata exportMetadata,
            ThreadExportDlpScanResult scanResult,
            boolean blocked
    ) {
        String categories = scanResult.matches().stream()
                .map(ThreadExportDlpMatch::category)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("policy");
        ThreadExportDlpEventEntity event = new ThreadExportDlpEventEntity();
        event.setOrganizationId(organizationId);
        event.setProjectId(projectId);
        event.setConversationId(conversationId);
        event.setExportId(exportMetadata.exportId());
        event.setExportedByUserId(exportMetadata.exportedByUserId());
        event.setMatchCategories(categories.length() > 512 ? categories.substring(0, 512) : categories);
        event.setBlocked(blocked);
        eventRepository.save(event);
    }

    private List<ThreadExportDlpCustomPattern> collectCustomPatterns(List<OrgDlpConnectorEntity> connectors) {
        List<ThreadExportDlpCustomPattern> patterns = new ArrayList<>();
        for (OrgDlpConnectorEntity connector : connectors) {
            if (connector.getCustomPatternsJson() == null || connector.getCustomPatternsJson().isBlank()) {
                continue;
            }
            try {
                List<ThreadExportDlpCustomPattern> parsed = objectMapper.readValue(
                        connector.getCustomPatternsJson(),
                        new TypeReference<List<ThreadExportDlpCustomPattern>>() {});
                if (parsed != null) {
                    patterns.addAll(parsed);
                }
            } catch (Exception ignored) {
                // skip invalid JSON
            }
        }
        return patterns;
    }
}
