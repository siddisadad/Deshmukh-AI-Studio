package com.aistudio.application.project;

import com.aistudio.api.organization.dto.OrganizationResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectAuthorizationService authorizationService;

    public OrganizationService(
            MembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listForUser(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID orgId, UUID userId) {
        MembershipEntity membership = authorizationService.requireOrgMember(orgId, userId);
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Organization not found"));
        return new OrganizationResponse(org.getId(), org.getName(), org.getSlug(), membership.getRole().name(), org.getCreatedAt());
    }

    private OrganizationResponse toResponse(MembershipEntity membership) {
        OrganizationEntity org = organizationRepository.findById(membership.getOrganizationId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Organization not found"));
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                membership.getRole().name(),
                org.getCreatedAt()
        );
    }
}
