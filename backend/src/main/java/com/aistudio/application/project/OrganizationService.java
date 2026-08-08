package com.aistudio.application.project;

import com.aistudio.api.organization.dto.AddMemberRequest;
import com.aistudio.api.organization.dto.MemberResponse;
import com.aistudio.api.organization.dto.OrganizationResponse;
import com.aistudio.application.billing.BillingService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.entity.UserEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import java.util.List;
import java.util.Locale;
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
    private final BillingService billingService;

    public OrganizationService(
            MembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            ProjectAuthorizationService authorizationService,
            BillingService billingService
    ) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.billingService = billingService;
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
        // Any org member may list (used by task assignee picker); invite remains ADMIN+.
        authorizationService.requireOrgMember(orgId, userId);
        List<MembershipEntity> memberships = membershipRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId);
        Map<UUID, UserEntity> users = userRepository.findAllById(
                memberships.stream().map(MembershipEntity::getUserId).toList()
        ).stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return memberships.stream()
                .map(membership -> toMemberResponse(membership, users))
                .toList();
    }

    @Transactional
    public MemberResponse addMember(UUID orgId, UUID actorUserId, AddMemberRequest request) {
        authorizationService.requireOrgOwner(orgId, actorUserId);
        if (request.role() == OrgRole.OWNER) {
            throw new DomainException("VALIDATION_ERROR", "Cannot assign OWNER via invite; transfer ownership is not supported in MVP");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserEntity invitee = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new DomainException(
                        "NOT_FOUND",
                        "No registered user with that email. MVP invite adds an existing AI Studio account."));

        if (membershipRepository.findByOrganizationIdAndUserId(orgId, invitee.getId()).isPresent()) {
            throw new DomainException("CONFLICT", "User is already a member of this organization");
        }

        billingService.requireCanAddMember(orgId);

        MembershipEntity membership = new MembershipEntity();
        membership.setOrganizationId(orgId);
        membership.setUserId(invitee.getId());
        membership.setRole(request.role());
        membershipRepository.save(membership);

        return new MemberResponse(
                invitee.getId(),
                invitee.getEmail(),
                invitee.getDisplayName(),
                membership.getRole().name()
        );
    }

    private MemberResponse toMemberResponse(MembershipEntity membership, Map<UUID, UserEntity> users) {
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
