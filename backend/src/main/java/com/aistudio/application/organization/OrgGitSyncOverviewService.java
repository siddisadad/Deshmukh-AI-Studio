package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.OrgGitSyncOverviewItemResponse;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectGitLinkRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgGitSyncOverviewService {

    private final ProjectRepository projectRepository;
    private final ProjectGitLinkRepository gitLinkRepository;
    private final ProjectAuthorizationService authorizationService;

    public OrgGitSyncOverviewService(
            ProjectRepository projectRepository,
            ProjectGitLinkRepository gitLinkRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.projectRepository = projectRepository;
        this.gitLinkRepository = gitLinkRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public OrgGitSyncOverviewResponse getOverview(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);

        List<ProjectEntity> projects = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        Map<UUID, ProjectGitLinkEntity> linksByProjectId = new HashMap<>();
        if (!projects.isEmpty()) {
            List<UUID> projectIds = projects.stream().map(ProjectEntity::getId).toList();
            for (ProjectGitLinkEntity link : gitLinkRepository.findByProjectIdIn(projectIds)) {
                linksByProjectId.put(link.getProjectId(), link);
            }
        }

        int linkedProjects = 0;
        int enabledLinks = 0;
        int failedLastSync = 0;
        List<OrgGitSyncOverviewItemResponse> items = projects.stream()
                .map(project -> toItem(project, linksByProjectId.get(project.getId())))
                .toList();

        for (OrgGitSyncOverviewItemResponse item : items) {
            if (item.linked()) {
                linkedProjects++;
                if (item.enabled()) {
                    enabledLinks++;
                }
                if ("failed".equals(item.lastSyncStatus())) {
                    failedLastSync++;
                }
            }
        }

        return new OrgGitSyncOverviewResponse(
                organizationId,
                projects.size(),
                linkedProjects,
                enabledLinks,
                failedLastSync,
                items
        );
    }

    private OrgGitSyncOverviewItemResponse toItem(ProjectEntity project, ProjectGitLinkEntity link) {
        if (link == null) {
            return new OrgGitSyncOverviewItemResponse(
                    project.getId(),
                    project.getName(),
                    project.getProjectKey(),
                    false,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    null,
                    "never",
                    null,
                    null
            );
        }
        return new OrgGitSyncOverviewItemResponse(
                project.getId(),
                project.getName(),
                project.getProjectKey(),
                true,
                link.getId(),
                link.getProvider(),
                link.getRepository(),
                link.getBranch(),
                link.isEnabled(),
                link.isScheduledSyncEnabled(),
                link.getLastSyncedAt(),
                link.getLastSyncStatus(),
                link.getLastSyncError(),
                link.getScheduledSyncIntervalMinutes()
        );
    }
}
