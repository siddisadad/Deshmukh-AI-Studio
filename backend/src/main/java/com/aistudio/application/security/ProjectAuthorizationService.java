package com.aistudio.application.security;

import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.domain.project.ProjectRole;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectMemberEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectMemberRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAuthorizationService {

    private static final Set<OrgRole> ORG_CAN_CREATE = EnumSet.of(OrgRole.OWNER, OrgRole.ADMIN, OrgRole.MEMBER);
    private static final Set<ProjectRole> PROJECT_CAN_EDIT = EnumSet.of(ProjectRole.OWNER, ProjectRole.ADMIN, ProjectRole.MEMBER);
    private static final Set<ProjectRole> PROJECT_CAN_ARCHIVE = EnumSet.of(ProjectRole.OWNER, ProjectRole.ADMIN);

    private final MembershipRepository membershipRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAuthorizationService(
            MembershipRepository membershipRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public MembershipEntity requireOrgMember(UUID orgId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(this::forbidden);
    }

    @Transactional(readOnly = true)
    public MembershipEntity requireOrgOwner(UUID orgId, UUID userId) {
        MembershipEntity membership = requireOrgMember(orgId, userId);
        if (membership.getRole() != OrgRole.OWNER && membership.getRole() != OrgRole.ADMIN) {
            throw forbidden();
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public MembershipEntity requireOrgCreateProject(UUID orgId, UUID userId) {
        MembershipEntity membership = requireOrgMember(orgId, userId);
        if (!ORG_CAN_CREATE.contains(membership.getRole())) {
            throw forbidden();
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public ProjectEntity requireProjectAccess(UUID projectId, UUID userId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(this::notFound);
        boolean projectMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
        boolean orgMember = membershipRepository.findByOrganizationIdAndUserId(project.getOrganizationId(), userId).isPresent();
        if (!projectMember && !orgMember) {
            throw notFound();
        }
        return project;
    }

    @Transactional(readOnly = true)
    public ProjectRole requireProjectEdit(UUID projectId, UUID userId) {
        ProjectRole role = resolveProjectRole(projectId, userId);
        if (!PROJECT_CAN_EDIT.contains(role)) {
            throw forbidden();
        }
        return role;
    }

    @Transactional(readOnly = true)
    public ProjectRole requireProjectArchive(UUID projectId, UUID userId) {
        ProjectRole role = resolveProjectRole(projectId, userId);
        if (!PROJECT_CAN_ARCHIVE.contains(role)) {
            throw forbidden();
        }
        return role;
    }

    @Transactional(readOnly = true)
    public ProjectRole resolveProjectRole(UUID projectId, UUID userId) {
        ProjectEntity project = requireProjectAccess(projectId, userId);
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMemberEntity::getRole)
                .orElseGet(() -> membershipRepository.findByOrganizationIdAndUserId(project.getOrganizationId(), userId)
                        .map(m -> mapOrgToProjectRole(m.getRole()))
                        .orElseThrow(this::notFound));
    }

    private static ProjectRole mapOrgToProjectRole(OrgRole orgRole) {
        return switch (orgRole) {
            case OWNER -> ProjectRole.OWNER;
            case ADMIN -> ProjectRole.ADMIN;
            case MEMBER -> ProjectRole.MEMBER;
            case VIEWER -> ProjectRole.VIEWER;
        };
    }

    private DomainException forbidden() {
        return new DomainException("FORBIDDEN", "You do not have permission to perform this action");
    }

    private DomainException notFound() {
        return new DomainException("NOT_FOUND", "Resource not found");
    }
}
