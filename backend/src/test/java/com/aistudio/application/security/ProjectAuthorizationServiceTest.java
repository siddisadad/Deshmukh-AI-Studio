package com.aistudio.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.domain.project.ProjectRole;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectMemberEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectMemberRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationServiceTest {

    @Mock MembershipRepository membershipRepository;
    @Mock ProjectRepository projectRepository;
    @Mock ProjectMemberRepository projectMemberRepository;

    ProjectAuthorizationService service;

    UUID orgId;
    UUID userId;
    UUID projectId;

    @BeforeEach
    void setUp() {
        service = new ProjectAuthorizationService(
                membershipRepository, projectRepository, projectMemberRepository);
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
    }

    @Test
    void requireOrgCreateProjectAllowsMember() {
        MembershipEntity membership = membership(OrgRole.MEMBER);
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership));

        assertThat(service.requireOrgCreateProject(orgId, userId)).isSameAs(membership);
    }

    @Test
    void requireOrgCreateProjectRejectsViewer() {
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership(OrgRole.VIEWER)));

        assertThatThrownBy(() -> service.requireOrgCreateProject(orgId, userId))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void requireOrgOwnerRejectsMember() {
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership(OrgRole.MEMBER)));

        assertThatThrownBy(() -> service.requireOrgOwner(orgId, userId))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void requireProjectAccessDeniedForOutsider() {
        ProjectEntity project = project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireProjectAccess(projectId, userId))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void resolveProjectRoleUsesProjectMembershipFirst() {
        ProjectEntity project = project();
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(ProjectRole.ADMIN);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(member));

        assertThat(service.resolveProjectRole(projectId, userId)).isEqualTo(ProjectRole.ADMIN);
    }

    @Test
    void resolveProjectRoleFallsBackToOrgRoleMapping() {
        ProjectEntity project = project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership(OrgRole.VIEWER)));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThat(service.resolveProjectRole(projectId, userId)).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    void requireProjectEditRejectsViewer() {
        ProjectEntity project = project();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(membershipRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(membership(OrgRole.VIEWER)));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireProjectEdit(projectId, userId))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    private MembershipEntity membership(OrgRole role) {
        MembershipEntity entity = new MembershipEntity();
        entity.setOrganizationId(orgId);
        entity.setUserId(userId);
        entity.setRole(role);
        return entity;
    }

    private ProjectEntity project() {
        ProjectEntity project = new ProjectEntity();
        project.setId(projectId);
        project.setOrganizationId(orgId);
        project.setName("Demo");
        project.setProjectKey("DEMO");
        return project;
    }
}
