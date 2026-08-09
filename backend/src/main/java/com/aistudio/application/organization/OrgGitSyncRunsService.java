package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.OrgGitSyncRunItemResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRunPageResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectGitSyncRunEntity;
import com.aistudio.infrastructure.persistence.repository.ProjectGitSyncRunRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgGitSyncRunsService {

    private final ProjectRepository projectRepository;
    private final ProjectGitSyncRunRepository syncRunRepository;
    private final ProjectAuthorizationService authorizationService;

    public OrgGitSyncRunsService(
            ProjectRepository projectRepository,
            ProjectGitSyncRunRepository syncRunRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.projectRepository = projectRepository;
        this.syncRunRepository = syncRunRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public OrgGitSyncRunPageResponse listRuns(
            UUID organizationId,
            UUID userId,
            int limit,
            int offset,
            String source,
            String status,
            UUID projectId
    ) {
        authorizationService.requireOrgMember(organizationId, userId);

        List<ProjectEntity> projects = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        Map<UUID, ProjectEntity> projectsById = new HashMap<>();
        for (ProjectEntity project : projects) {
            projectsById.put(project.getId(), project);
        }

        List<UUID> projectIds;
        if (projectId != null) {
            if (!projectsById.containsKey(projectId)) {
                throw new DomainException("VALIDATION_ERROR", "projectId is not in this organization");
            }
            projectIds = List.of(projectId);
        } else {
            projectIds = projects.stream().map(ProjectEntity::getId).toList();
        }

        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int safeOffset = offset < 0 ? 0 : offset;
        if (safeOffset % safeLimit != 0) {
            throw new DomainException("VALIDATION_ERROR", "offset must be a multiple of limit");
        }

        if (projectIds.isEmpty()) {
            return new OrgGitSyncRunPageResponse(List.of(), safeOffset, safeLimit, 0, false);
        }

        String normalizedSource = normalizeSyncRunFilter(source, "source");
        String normalizedStatus = normalizeSyncRunFilter(status, "status");
        int pageNumber = safeOffset / safeLimit;
        PageRequest page = PageRequest.of(pageNumber, safeLimit);

        List<ProjectGitSyncRunEntity> runs;
        long totalCount;
        if (normalizedSource != null && normalizedStatus != null) {
            runs = syncRunRepository.findByProjectIdInAndSourceAndStatusOrderByFinishedAtDesc(
                    projectIds,
                    normalizedSource,
                    normalizedStatus,
                    page
            );
            totalCount = syncRunRepository.countByProjectIdInAndSourceAndStatus(
                    projectIds,
                    normalizedSource,
                    normalizedStatus
            );
        } else if (normalizedSource != null) {
            runs = syncRunRepository.findByProjectIdInAndSourceOrderByFinishedAtDesc(
                    projectIds,
                    normalizedSource,
                    page
            );
            totalCount = syncRunRepository.countByProjectIdInAndSource(projectIds, normalizedSource);
        } else if (normalizedStatus != null) {
            runs = syncRunRepository.findByProjectIdInAndStatusOrderByFinishedAtDesc(
                    projectIds,
                    normalizedStatus,
                    page
            );
            totalCount = syncRunRepository.countByProjectIdInAndStatus(projectIds, normalizedStatus);
        } else {
            runs = syncRunRepository.findByProjectIdInOrderByFinishedAtDesc(projectIds, page);
            totalCount = syncRunRepository.countByProjectIdIn(projectIds);
        }

        List<OrgGitSyncRunItemResponse> items = runs.stream()
                .map(run -> toItem(run, projectsById.get(run.getProjectId())))
                .toList();
        boolean hasMore = safeOffset + items.size() < totalCount;
        return new OrgGitSyncRunPageResponse(items, safeOffset, safeLimit, totalCount, hasMore);
    }

    private OrgGitSyncRunItemResponse toItem(ProjectGitSyncRunEntity run, ProjectEntity project) {
        String projectName = project != null ? project.getName() : "Unknown";
        String projectKey = project != null ? project.getProjectKey() : "";
        return new OrgGitSyncRunItemResponse(
                run.getId(),
                run.getProjectId(),
                projectName,
                projectKey,
                run.getGitLinkId(),
                run.getSource(),
                run.getStatus(),
                run.getFileCount(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getFinishedAt()
        );
    }

    private static String normalizeSyncRunFilter(String value, String field) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("source".equals(field)) {
            if ("manual".equals(normalized) || "scheduled".equals(normalized) || "webhook".equals(normalized)) {
                return normalized;
            }
            throw new DomainException("VALIDATION_ERROR", "source filter must be manual, scheduled, or webhook");
        }
        if ("success".equals(normalized) || "failed".equals(normalized)) {
            return normalized;
        }
        throw new DomainException("VALIDATION_ERROR", "status filter must be success or failed");
    }
}
