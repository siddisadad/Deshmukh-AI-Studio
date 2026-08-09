package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.CreateOrgDlpConnectorRequest;
import com.aistudio.api.organization.dto.OrgDlpConnectorResponse;
import com.aistudio.api.organization.dto.ThreadExportDlpEventResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.export.OrgDlpConnectorType;
import com.aistudio.infrastructure.persistence.entity.OrgDlpConnectorEntity;
import com.aistudio.infrastructure.persistence.entity.ThreadExportDlpEventEntity;
import com.aistudio.infrastructure.persistence.repository.OrgDlpConnectorRepository;
import com.aistudio.infrastructure.persistence.repository.ThreadExportDlpEventRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgDlpConnectorService {

    private final OrgDlpConnectorRepository connectorRepository;
    private final ThreadExportDlpEventRepository eventRepository;
    private final ProjectAuthorizationService authorizationService;

    public OrgDlpConnectorService(
            OrgDlpConnectorRepository connectorRepository,
            ThreadExportDlpEventRepository eventRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.connectorRepository = connectorRepository;
        this.eventRepository = eventRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<OrgDlpConnectorResponse> listConnectors(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return connectorRepository.findByOrganizationIdOrderByDisplayNameAsc(organizationId).stream()
                .map(this::toConnectorResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ThreadExportDlpEventResponse> listEvents(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return eventRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Transactional
    public OrgDlpConnectorResponse createConnector(
            UUID organizationId,
            UUID userId,
            CreateOrgDlpConnectorRequest request
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (connectorRepository.findByOrganizationIdAndSlug(organizationId, slug).isPresent()) {
            throw new DomainException("VALIDATION_ERROR", "DLP connector slug already exists");
        }
        OrgDlpConnectorEntity connector = new OrgDlpConnectorEntity();
        connector.setOrganizationId(organizationId);
        connector.setSlug(slug);
        connector.setConnectorType(parseType(request.connectorType()));
        connector.setDisplayName(request.displayName().trim());
        connector.setWebhookUrl(request.webhookUrl().trim());
        connector.setEnabled(request.enabled());
        connector.setBlockOnMatch(request.blockOnMatch());
        connector.setCustomPatternsJson(blankToNull(request.customPatternsJson()));
        return toConnectorResponse(connectorRepository.save(connector));
    }

    @Transactional
    public void deleteConnector(UUID organizationId, UUID connectorId, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        OrgDlpConnectorEntity connector = connectorRepository.findByIdAndOrganizationId(connectorId, organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "DLP connector not found"));
        connectorRepository.delete(connector);
    }

    private static OrgDlpConnectorType parseType(String raw) {
        try {
            return OrgDlpConnectorType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "connectorType must be WEBHOOK or SIEM");
        }
    }

    private OrgDlpConnectorResponse toConnectorResponse(OrgDlpConnectorEntity connector) {
        return new OrgDlpConnectorResponse(
                connector.getId(),
                connector.getSlug(),
                connector.getConnectorType().name(),
                connector.getDisplayName(),
                connector.getWebhookUrl(),
                connector.isEnabled(),
                connector.isBlockOnMatch(),
                connector.getCustomPatternsJson(),
                connector.getCreatedAt(),
                connector.getUpdatedAt()
        );
    }

    private ThreadExportDlpEventResponse toEventResponse(ThreadExportDlpEventEntity event) {
        return new ThreadExportDlpEventResponse(
                event.getId(),
                event.getProjectId(),
                event.getConversationId(),
                event.getExportId(),
                event.getExportedByUserId(),
                event.getMatchCategories(),
                event.isBlocked(),
                event.getSiemExportedAt(),
                event.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
