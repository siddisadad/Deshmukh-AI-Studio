package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.OrgGitSyncClearIntervalProjectResponse;
import com.aistudio.api.organization.dto.OrgGitSyncDisableScheduledResponse;
import com.aistudio.api.organization.dto.OrgGitSyncEnableScheduledResponse;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewExport;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewItemResponse;
import com.aistudio.api.organization.dto.OrgGitSyncOverviewResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRetryProjectResponse;
import com.aistudio.api.organization.dto.OrgGitSyncScheduledProjectResponse;
import com.aistudio.api.organization.dto.OrgGitSyncSetIntervalProjectResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRetryFailedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    private final ObjectMapper objectMapper;

    public OrgGitSyncOverviewService(
            ProjectRepository projectRepository,
            ProjectGitLinkRepository gitLinkRepository,
            ProjectAuthorizationService authorizationService,
            BackgroundJobService backgroundJobService,
            BackgroundJobRepository backgroundJobRepository,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.gitLinkRepository = gitLinkRepository;
        this.authorizationService = authorizationService;
        this.backgroundJobService = backgroundJobService;
        this.backgroundJobRepository = backgroundJobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public OrgGitSyncOverviewResponse getOverview(
            UUID organizationId,
            UUID userId,
            Boolean linked,
            Boolean enabled,
            Boolean scheduledSyncEnabled,
            Boolean customSyncInterval,
            String provider,
            String lastSyncStatus
    ) {
        authorizationService.requireOrgMember(organizationId, userId);
        String normalizedProvider = normalizeProvider(provider);
        String normalizedLastSyncStatus = normalizeLastSyncStatus(lastSyncStatus);

        List<OrgGitSyncOverviewItemResponse> items = buildOverviewItems(organizationId);

        int linkedProjects = 0;
        int enabledLinks = 0;
        int scheduledSyncLinks = 0;
        int manualSyncLinks = 0;
        int customSyncIntervalLinks = 0;
        int failedLastSync = 0;
        for (OrgGitSyncOverviewItemResponse item : items) {
            if (item.linked()) {
                linkedProjects++;
                if (item.enabled()) {
                    enabledLinks++;
                    if (item.scheduledSyncEnabled()) {
                        scheduledSyncLinks++;
                    } else {
                        manualSyncLinks++;
                    }
                    if (item.scheduledSyncIntervalMinutes() != null) {
                        customSyncIntervalLinks++;
                    }
                }
                if ("failed".equals(item.lastSyncStatus())) {
                    failedLastSync++;
                }
            }
        }

        List<OrgGitSyncOverviewItemResponse> filteredItems = items.stream()
                .filter(item -> matchesFilters(
                        item,
                        linked,
                        enabled,
                        scheduledSyncEnabled,
                        customSyncInterval,
                        normalizedProvider,
                        normalizedLastSyncStatus))
                .toList();

        return new OrgGitSyncOverviewResponse(
                organizationId,
                items.size(),
                linkedProjects,
                enabledLinks,
                scheduledSyncLinks,
                manualSyncLinks,
                customSyncIntervalLinks,
                failedLastSync,
                filteredItems
        );
    }

    @Transactional
    public OrgGitSyncEnableScheduledResponse enableScheduledSyncs(
            UUID organizationId,
            UUID userId,
            Boolean linked,
            Boolean enabled,
            Boolean scheduledSyncEnabled,
            Boolean customSyncInterval,
            String provider,
            String lastSyncStatus
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String normalizedProvider = normalizeProvider(provider);
        String normalizedLastSyncStatus = normalizeLastSyncStatus(lastSyncStatus);

        List<OrgGitSyncOverviewItemResponse> manualItems = buildOverviewItems(organizationId).stream()
                .filter(item -> item.linked() && item.enabled() && !item.scheduledSyncEnabled())
                .filter(item -> matchesFilters(
                        item,
                        linked,
                        enabled,
                        scheduledSyncEnabled,
                        customSyncInterval,
                        normalizedProvider,
                        normalizedLastSyncStatus))
                .toList();

        List<UUID> updatedProjectIds = new ArrayList<>();
        for (OrgGitSyncOverviewItemResponse item : manualItems) {
            ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(item.projectId())
                    .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Project has no git link"));
            link.setScheduledSyncEnabled(true);
            gitLinkRepository.save(link);
            updatedProjectIds.add(item.projectId());
        }

        return new OrgGitSyncEnableScheduledResponse(
                manualItems.size(),
                updatedProjectIds.size(),
                updatedProjectIds
        );
    }

    @Transactional
    public OrgGitSyncDisableScheduledResponse disableScheduledSyncs(
            UUID organizationId,
            UUID userId,
            Boolean linked,
            Boolean enabled,
            Boolean scheduledSyncEnabled,
            Boolean customSyncInterval,
            String provider,
            String lastSyncStatus
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String normalizedProvider = normalizeProvider(provider);
        String normalizedLastSyncStatus = normalizeLastSyncStatus(lastSyncStatus);

        List<OrgGitSyncOverviewItemResponse> scheduledItems = buildOverviewItems(organizationId).stream()
                .filter(item -> item.linked() && item.enabled() && item.scheduledSyncEnabled())
                .filter(item -> matchesFilters(
                        item,
                        linked,
                        enabled,
                        scheduledSyncEnabled,
                        customSyncInterval,
                        normalizedProvider,
                        normalizedLastSyncStatus))
                .toList();

        List<UUID> updatedProjectIds = new ArrayList<>();
        for (OrgGitSyncOverviewItemResponse item : scheduledItems) {
            ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(item.projectId())
                    .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Project has no git link"));
            link.setScheduledSyncEnabled(false);
            gitLinkRepository.save(link);
            updatedProjectIds.add(item.projectId());
        }

        return new OrgGitSyncDisableScheduledResponse(
                scheduledItems.size(),
                updatedProjectIds.size(),
                updatedProjectIds
        );
    }

    @Transactional
    public OrgGitSyncRetryFailedResponse retryFailedSyncs(
            UUID organizationId,
            UUID userId,
            Boolean linked,
            Boolean enabled,
            Boolean scheduledSyncEnabled,
            Boolean customSyncInterval,
            String provider,
            String lastSyncStatus
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String normalizedProvider = normalizeProvider(provider);
        String normalizedLastSyncStatus = normalizeLastSyncStatus(lastSyncStatus);

        List<OrgGitSyncOverviewItemResponse> failedItems = buildOverviewItems(organizationId).stream()
                .filter(item -> item.linked() && item.enabled() && "failed".equals(item.lastSyncStatus()))
                .filter(item -> matchesFilters(
                        item,
                        linked,
                        enabled,
                        scheduledSyncEnabled,
                        customSyncInterval,
                        normalizedProvider,
                        normalizedLastSyncStatus))
                .toList();

        int skippedPending = 0;
        List<UUID> enqueuedProjectIds = new ArrayList<>();
        for (OrgGitSyncOverviewItemResponse item : failedItems) {
            UUID projectId = item.projectId();
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
                failedItems.size(),
                enqueuedProjectIds.size(),
                skippedPending,
                enqueuedProjectIds
        );
    }

    @Transactional
    public OrgGitSyncRetryProjectResponse retryFailedSyncForProject(
            UUID organizationId,
            UUID projectId,
            UUID userId
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        if (!project.getOrganizationId().equals(organizationId)) {
            throw new DomainException("VALIDATION_ERROR", "projectId is not in this organization");
        }

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Project has no git link"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }
        if (!"failed".equals(link.getLastSyncStatus())) {
            throw new DomainException("VALIDATION_ERROR", "Project last sync status is not failed");
        }

        if (backgroundJobRepository.countByProjectIdAndJobTypeAndStatus(
                projectId,
                JobType.CODE_METADATA_SYNC,
                JobStatus.PENDING
        ) > 0) {
            return new OrgGitSyncRetryProjectResponse(projectId, false, true);
        }

        backgroundJobService.enqueueInternal(
                projectId,
                userId,
                JobType.CODE_METADATA_SYNC,
                "{\"source\":\"manual\"}"
        );
        return new OrgGitSyncRetryProjectResponse(projectId, true, false);
    }

    @Transactional
    public OrgGitSyncScheduledProjectResponse enableScheduledSyncForProject(
            UUID organizationId,
            UUID projectId,
            UUID userId
    ) {
        return setScheduledSyncForProject(organizationId, projectId, userId, true);
    }

    @Transactional
    public OrgGitSyncScheduledProjectResponse disableScheduledSyncForProject(
            UUID organizationId,
            UUID projectId,
            UUID userId
    ) {
        return setScheduledSyncForProject(organizationId, projectId, userId, false);
    }

    @Transactional
    public OrgGitSyncClearIntervalProjectResponse clearCustomSyncIntervalForProject(
            UUID organizationId,
            UUID projectId,
            UUID userId
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        if (!project.getOrganizationId().equals(organizationId)) {
            throw new DomainException("VALIDATION_ERROR", "projectId is not in this organization");
        }

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Project has no git link"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }

        if (link.getScheduledSyncIntervalMinutes() == null) {
            return new OrgGitSyncClearIntervalProjectResponse(projectId, null, false);
        }

        link.setScheduledSyncIntervalMinutes(null);
        gitLinkRepository.save(link);
        return new OrgGitSyncClearIntervalProjectResponse(projectId, null, true);
    }

    @Transactional
    public OrgGitSyncSetIntervalProjectResponse setCustomSyncIntervalForProject(
            UUID organizationId,
            UUID projectId,
            UUID userId,
            int scheduledSyncIntervalMinutes
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);

        if (scheduledSyncIntervalMinutes < 15 || scheduledSyncIntervalMinutes > 10080) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "scheduledSyncIntervalMinutes must be between 15 and 10080");
        }

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        if (!project.getOrganizationId().equals(organizationId)) {
            throw new DomainException("VALIDATION_ERROR", "projectId is not in this organization");
        }

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Project has no git link"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }

        if (link.getScheduledSyncIntervalMinutes() != null
                && link.getScheduledSyncIntervalMinutes() == scheduledSyncIntervalMinutes) {
            return new OrgGitSyncSetIntervalProjectResponse(
                    projectId,
                    scheduledSyncIntervalMinutes,
                    false);
        }

        link.setScheduledSyncIntervalMinutes(scheduledSyncIntervalMinutes);
        gitLinkRepository.save(link);
        return new OrgGitSyncSetIntervalProjectResponse(
                projectId,
                scheduledSyncIntervalMinutes,
                true);
    }

    private OrgGitSyncScheduledProjectResponse setScheduledSyncForProject(
            UUID organizationId,
            UUID projectId,
            UUID userId,
            boolean scheduledSyncEnabled
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Project not found"));
        if (!project.getOrganizationId().equals(organizationId)) {
            throw new DomainException("VALIDATION_ERROR", "projectId is not in this organization");
        }

        ProjectGitLinkEntity link = gitLinkRepository.findByProjectId(projectId)
                .orElseThrow(() -> new DomainException("VALIDATION_ERROR", "Project has no git link"));
        if (!link.isEnabled()) {
            throw new DomainException("VALIDATION_ERROR", "Git link is disabled");
        }

        if (link.isScheduledSyncEnabled() == scheduledSyncEnabled) {
            return new OrgGitSyncScheduledProjectResponse(projectId, scheduledSyncEnabled, false);
        }

        link.setScheduledSyncEnabled(scheduledSyncEnabled);
        gitLinkRepository.save(link);
        return new OrgGitSyncScheduledProjectResponse(projectId, scheduledSyncEnabled, true);
    }

    @Transactional(readOnly = true)
    public OrgGitSyncOverviewExport exportOverview(
            UUID organizationId,
            UUID userId,
            String format,
            Boolean linked,
            Boolean enabled,
            Boolean scheduledSyncEnabled,
            Boolean customSyncInterval,
            String provider,
            String lastSyncStatus
    ) {
        OrgGitSyncOverviewResponse overview = getOverview(
                organizationId,
                userId,
                linked,
                enabled,
                scheduledSyncEnabled,
                customSyncInterval,
                provider,
                lastSyncStatus);
        String normalizedFormat = normalizeExportFormat(format);
        if ("json".equals(normalizedFormat)) {
            return exportAsJson(overview);
        }
        return exportAsCsv(overview);
    }

    private boolean matchesFilters(
            OrgGitSyncOverviewItemResponse item,
            Boolean linked,
            Boolean enabled,
            Boolean scheduledSyncEnabled,
            Boolean customSyncInterval,
            String provider,
            String lastSyncStatus
    ) {
        if (linked != null && item.linked() != linked) {
            return false;
        }
        if (enabled != null) {
            if (!item.linked()) {
                return false;
            }
            if (item.enabled() != enabled) {
                return false;
            }
        }
        if (scheduledSyncEnabled != null) {
            if (!item.linked()) {
                return false;
            }
            if (item.scheduledSyncEnabled() != scheduledSyncEnabled) {
                return false;
            }
        }
        if (customSyncInterval != null) {
            if (!item.linked()) {
                return false;
            }
            boolean hasCustomInterval = item.scheduledSyncIntervalMinutes() != null;
            if (hasCustomInterval != customSyncInterval) {
                return false;
            }
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

    private String normalizeExportFormat(String format) {
        if (format == null || format.isBlank()) {
            return "csv";
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        if (!"csv".equals(normalized) && !"json".equals(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "format must be csv or json");
        }
        return normalized;
    }

    private List<OrgGitSyncOverviewItemResponse> buildOverviewItems(UUID organizationId) {
        List<ProjectEntity> projects = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        Map<UUID, ProjectGitLinkEntity> linksByProjectId = new HashMap<>();
        if (!projects.isEmpty()) {
            List<UUID> projectIds = projects.stream().map(ProjectEntity::getId).toList();
            for (ProjectGitLinkEntity link : gitLinkRepository.findByProjectIdIn(projectIds)) {
                linksByProjectId.put(link.getProjectId(), link);
            }
        }
        return projects.stream()
                .map(project -> toItem(project, linksByProjectId.get(project.getId())))
                .toList();
    }

    private OrgGitSyncOverviewExport exportAsJson(OrgGitSyncOverviewResponse overview) {
        try {
            byte[] body = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsBytes(overview);
            return new OrgGitSyncOverviewExport(
                    body,
                    "application/json; charset=UTF-8",
                    "git-sync-overview-" + overview.organizationId() + ".json"
            );
        } catch (Exception ex) {
            throw new DomainException("INTERNAL_ERROR", "Failed to export git sync overview as JSON");
        }
    }

    private OrgGitSyncOverviewExport exportAsCsv(OrgGitSyncOverviewResponse overview) {
        StringBuilder csv = new StringBuilder();
        csv.append("projectId,projectName,projectKey,linked,provider,repository,branch,enabled,scheduledSyncEnabled,lastSyncedAt,lastSyncStatus,lastSyncError,scheduledSyncIntervalMinutes\n");
        for (OrgGitSyncOverviewItemResponse item : overview.items()) {
            csv.append(csvCell(item.projectId()))
                    .append(',').append(csvCell(item.projectName()))
                    .append(',').append(csvCell(item.projectKey()))
                    .append(',').append(item.linked())
                    .append(',').append(csvCell(item.provider()))
                    .append(',').append(csvCell(item.repository()))
                    .append(',').append(csvCell(item.branch()))
                    .append(',').append(item.enabled())
                    .append(',').append(item.scheduledSyncEnabled())
                    .append(',').append(csvCell(item.lastSyncedAt()))
                    .append(',').append(csvCell(item.lastSyncStatus()))
                    .append(',').append(csvCell(item.lastSyncError()))
                    .append(',').append(csvCell(item.scheduledSyncIntervalMinutes()))
                    .append('\n');
        }
        return new OrgGitSyncOverviewExport(
                csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "text/csv; charset=UTF-8",
                "git-sync-overview-" + overview.organizationId() + ".csv"
        );
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
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
