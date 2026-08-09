package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.OrgSloSettingsResponse;
import com.aistudio.api.organization.dto.UpdateOrgSloSettingsRequest;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgSloService {

    private final OrganizationRepository organizationRepository;
    private final ProjectAuthorizationService authorizationService;

    public OrgSloService(
            OrganizationRepository organizationRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public OrgSloSettingsResponse getSettings(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return toResponse(requireOrganization(organizationId));
    }

    @Transactional
    public OrgSloSettingsResponse updateSettings(
            UUID organizationId,
            UUID userId,
            UpdateOrgSloSettingsRequest request
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        OrganizationEntity org = requireOrganization(organizationId);
        org.setSloAvailabilityTarget(request.availabilityTarget());
        org.setSloLatencyTarget(request.latencyTarget());
        org.setSloLatencyThresholdSeconds(request.latencyThresholdSeconds());
        organizationRepository.save(org);
        return toResponse(org);
    }

    private OrganizationEntity requireOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Organization not found"));
    }

    private static OrgSloSettingsResponse toResponse(OrganizationEntity org) {
        return new OrgSloSettingsResponse(
                org.getSloAvailabilityTarget(),
                org.getSloLatencyTarget(),
                org.getSloLatencyThresholdSeconds()
        );
    }
}
