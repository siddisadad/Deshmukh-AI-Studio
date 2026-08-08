package com.aistudio.application.project;

import com.aistudio.api.project.dto.CreateProjectRequest;
import com.aistudio.api.project.dto.ProjectResponse;
import com.aistudio.api.project.dto.UpdateProjectRequest;
import com.aistudio.application.audit.AuditService;
import com.aistudio.application.billing.BillingService;
import com.aistudio.application.ai.ConversationService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.project.ProjectRole;
import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectMemberEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectMemberRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAuthorizationService authorizationService;
    private final AuditService auditService;
    private final BillingService billingService;
    private final ConversationService conversationService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectAuthorizationService authorizationService,
            AuditService auditService,
            BillingService billingService,
            ConversationService conversationService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.billingService = billingService;
        this.conversationService = conversationService;
    }

    @Transactional
    public ProjectResponse create(UUID orgId, UUID userId, CreateProjectRequest request, String ip) {
        authorizationService.requireOrgCreateProject(orgId, userId);
        billingService.requireCanCreateProject(orgId);
        String key = request.projectKey().toUpperCase(Locale.ROOT);
        if (projectRepository.existsByOrganizationIdAndProjectKeyIgnoreCase(orgId, key)) {
            throw new DomainException("PROJECT_KEY_TAKEN", "Project key already exists in this organization");
        }

        ProjectEntity project = new ProjectEntity();
        project.setOrganizationId(orgId);
        project.setName(request.name().trim());
        project.setProjectKey(key);
        project.setDescription(request.description());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(userId);
        projectRepository.save(project);

        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(project.getId());
        member.setUserId(userId);
        member.setRole(ProjectRole.OWNER);
        projectMemberRepository.save(member);

        auditService.record(userId, "PROJECT_CREATED", "PROJECT", project.getId(), "{}", ip);
        return toResponse(project, ProjectRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listByOrg(UUID orgId, UUID userId, String statusFilter) {
        authorizationService.requireOrgMember(orgId, userId);
        List<ProjectEntity> projects;
        if (statusFilter == null || statusFilter.isBlank() || "ACTIVE".equalsIgnoreCase(statusFilter)) {
            projects = projectRepository.findByOrganizationIdAndStatusOrderByUpdatedAtDesc(orgId, ProjectStatus.ACTIVE);
        } else if ("ARCHIVED".equalsIgnoreCase(statusFilter)) {
            projects = projectRepository.findByOrganizationIdAndStatusOrderByUpdatedAtDesc(orgId, ProjectStatus.ARCHIVED);
        } else if ("ALL".equalsIgnoreCase(statusFilter)) {
            projects = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(orgId);
        } else {
            throw new DomainException("VALIDATION_ERROR", "status must be ACTIVE, ARCHIVED, or ALL");
        }
        return projects.stream()
                .map(p -> toResponse(p, authorizationService.resolveProjectRole(p.getId(), userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID userId) {
        ProjectEntity project = authorizationService.requireProjectAccess(projectId, userId);
        return toResponse(project, authorizationService.resolveProjectRole(projectId, userId));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UUID userId, UpdateProjectRequest request) {
        authorizationService.requireProjectEdit(projectId, userId);
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.projectKey() != null && !request.projectKey().isBlank()) {
            String key = request.projectKey().toUpperCase(Locale.ROOT);
            if (!key.equalsIgnoreCase(project.getProjectKey())
                    && projectRepository.existsByOrganizationIdAndProjectKeyIgnoreCase(project.getOrganizationId(), key)) {
                throw new DomainException("PROJECT_KEY_TAKEN", "Project key already exists in this organization");
            }
            project.setProjectKey(key);
        }
        boolean retentionPolicyChanged = false;
        if (Boolean.TRUE.equals(request.clearChatRetention())) {
            project.setChatRetentionDays(null);
            retentionPolicyChanged = true;
        } else if (request.chatRetentionDays() != null) {
            project.setChatRetentionDays(request.chatRetentionDays());
            retentionPolicyChanged = true;
        }
        projectRepository.save(project);
        if (retentionPolicyChanged) {
            conversationService.reapplyProjectRetentionPolicy(projectId);
        }
        return toResponse(project, authorizationService.resolveProjectRole(projectId, userId));
    }

    @Transactional
    public ProjectResponse archive(UUID projectId, UUID userId, String ip) {
        authorizationService.requireProjectArchive(projectId, userId);
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        project.setStatus(ProjectStatus.ARCHIVED);
        project.setArchivedAt(Instant.now());
        projectRepository.save(project);
        auditService.record(userId, "PROJECT_ARCHIVED", "PROJECT", projectId, "{}", ip);
        return toResponse(project, authorizationService.resolveProjectRole(projectId, userId));
    }

    @Transactional
    public ProjectResponse unarchive(UUID projectId, UUID userId, String ip) {
        authorizationService.requireProjectArchive(projectId, userId);
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        project.setStatus(ProjectStatus.ACTIVE);
        project.setArchivedAt(null);
        projectRepository.save(project);
        auditService.record(userId, "PROJECT_UNARCHIVED", "PROJECT", projectId, "{}", ip);
        return toResponse(project, authorizationService.resolveProjectRole(projectId, userId));
    }

    private static ProjectResponse toResponse(ProjectEntity project, ProjectRole role) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganizationId(),
                project.getName(),
                project.getProjectKey(),
                project.getDescription(),
                project.getStatus().name(),
                role.name(),
                project.getChatRetentionDays(),
                project.getArchivedAt(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
