package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.OrgGitSyncOverviewItemResponse;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRetryFailedResponse;
import com.aistudio.application.job.BackgroundJobService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.job.JobStatus;
import com.aistudio.domain.job.JobType;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import com.aistudio.infrastructure.persistence.repository.BackgroundJobRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectGitLinkRepository;
import com.aistudio.infrastructure.persistence.repository.ProjectRepository;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgGitSyncOverviewService {

    private static final Set<String> ALLOWED_PROVIDERS = Set.of("github", "gitlab", "bitbucket");
    private static final Set<String> ALLOWED_LAST_SYNC_STATUSES = Set.of("success", "failed", "never");

    private final ProjectRepository projectRepository;
    private final ProjectGitLinkRepository gitLinkRepository;
    private final ProjectAuthorizationService authorizationService;
    private final BackgroundJobService backgroundJobService;
    private final BackgroundJobRepository backgroundJobRepository;

    public OrgGitSyncOverviewService(
            ProjectRepository projectRepository,
            ProjectGitLinkRepository gitLinkRepository,
            ProjectAuthorizationService authorizationService,
            BackgroundJobService backgroundJobService,
            BackgroundJobRepository backgroundJobRepository
    ) {
        this.projectRepository = projectRepository;
        this.gitLinkRepository = gitLinkRepository;
        this.authorizationService = authorizationService;
        this.backgroundJobService = backgroundJobService;
        this.backgroundJobRepository = backgroundJobRepository;
    }

    @Transactional(readOnly = true)
    public OrgGitSyncOverviewResponse getOverview(
            UUID organizationId,
            UUID userId,
            Boolean linked,
            String provider,
            String lastSyncStatus
    ) {
        authorizationService.requireOrgMember(organizationId, userId);
        String normalizedProvider = normalizeProvider(provider);
        String normalizedLastSyncStatus = normalizeLastSyncStatus(lastSyncStatus);

        List<ProjectEntity> projects = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        Map<UUID, ProjectGitLinkEntity> linksByProjectId = new HashMap<>();
        if (!projects.isEmpty()) {
            List<UUID> projectIds = projects.stream().map(ProjectEntity::getId).toList();
            for (ProjectGitLinkEntity link : gitLinkRepository.findByProjectIdIn(projectIds)) {
                linksByProjectId.put(link.getProjectId(), link);
            }
        }

        List<OrgGitSyncOverviewItemResponse> items = projects.stream()
                .map(project -> toItem(project, linksByProjectId.get(project.getId())))
                .toList();

        int linkedProjects = 0;
        int enabledLinks = 0;
        int failedLastSync = 0;
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

        List<OrgGitSyncOverviewItemResponse> filteredItems = items.stream()
                .filter(item -> matchesFilters(item, linked, normalizedProvider, normalizedLastSyncStatus))
                .toList();

        return new OrgGitSyncOverviewResponse(
                organizationId,
                projects.size(),
                linkedProjects,
                enabledLinks,
                failedLastSync,
                filteredItems
        );
    }

    @Transactional
    public OrgGitSyncRetryFailedResponse retryFailedSyncs(UUID organizationId, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);

        List<ProjectEntity> projects = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        Map<UUID, ProjectGitLinkEntity> linksByProjectId = new HashMap<>();
        if (!projects.isEmpty()) {
            List<UUID> projectIds = projects.stream().map(ProjectEntity::getId).toList();
            for (ProjectGitLinkEntity link : gitLinkRepository.findByProjectIdIn(projectIds)) {
                linksByProjectId.put(link.getProjectId(), link);
            }
        }

        List<ProjectGitLinkEntity> failedLinks = linksByProjectId.values().stream()
                .filter(link -> link.isEnabled() && "failed".equals(link.getLastSyncStatus()))
                .toList();

        int skippedPending = 0;
        List<UUID> enqueuedProjectIds = new ArrayList<>();
        for (ProjectGitLinkEntity link : failedLinks) {
            UUID projectId = link.getProjectId();
            if (backgroundJobRepository.countByProjectIdAndJobTypeAndStatus(
                    projectId,
                    JobType.CODE_METADATA_SYNC,
                    JobStatus.PENDING
            ) > 0) {
                skippedPending++;
                continue;
            }
            backgroundJobService.enqueueInternal(
                    projectId,
                    userId,
                    JobType.CODE_METADATA_SYNC,
                    "{\"source\":\"manual\"}"
            );
            enqueuedProjectIds.add(projectId);
        }

        return new OrgGitSyncRetryFailedResponse(
                failedLinks.size(),
                enqueuedProjectIds.size(),
                skippedPending,
                enqueuedProjectIds
        );
    }

    private boolean matchesFilters(
            OrgGitSyncOverviewItemResponse item,
            Boolean linked,
            String provider,
            String lastSyncStatus
    ) {
        if (linked != null && item.linked() != linked) {
            return false;
        }
        if (provider != null) {
            if (!item.linked() || item.provider() == null) {
                return false;
            }
            if (!provider.equals(item.provider().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (lastSyncStatus != null) {
            String itemStatus = item.lastSyncStatus() == null ? "never" : item.lastSyncStatus().toLowerCase(Locale.ROOT);
            if (!lastSyncStatus.equals(itemStatus)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_PROVIDERS.contains(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "provider filter must be github, gitlab, or bitbucket");
        }
        return normalized;
    }

    private String normalizeLastSyncStatus(String lastSyncStatus) {
        if (lastSyncStatus == null || lastSyncStatus.isBlank()) {
            return null;
        }
        String normalized = lastSyncStatus.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_LAST_SYNC_STATUSES.contains(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "lastSyncStatus filter must be success, failed, or never");
        }
        return normalized;
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
