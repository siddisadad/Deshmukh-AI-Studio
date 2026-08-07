package com.aistudio.application.project;

import com.aistudio.api.organization.dto.MemberResponse;
import com.aistudio.api.organization.dto.OrganizationResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.entity.UserEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectAuthorizationService authorizationService;

    public OrganizationService(
            MembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
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

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID orgId, UUID userId) {
        authorizationService.requireOrgMember(orgId, userId);
        List<MembershipEntity> memberships = membershipRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId);
        Map<UUID, UserEntity> users = userRepository.findAllById(
                memberships.stream().map(MembershipEntity::getUserId).toList()
        ).stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return memberships.stream()
                .map(membership -> {
                    UserEntity user = users.get(membership.getUserId());
                    if (user == null) {
                        throw new DomainException("NOT_FOUND", "Member user not found");
                    }
                    return new MemberResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getDisplayName(),
                            membership.getRole().name()
                    );
                })
                .toList();
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
