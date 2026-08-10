package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.OrgGitSyncRunExport;
import com.aistudio.api.organization.dto.OrgGitSyncRunExportPayload;
import com.aistudio.api.organization.dto.OrgGitSyncRunFilterCountsResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRunItemResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRunPageResponse;
import com.aistudio.api.organization.dto.OrgGitSyncRunPresetCountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

    private static final int MAX_EXPORT_ROWS = 1000;

    private final ProjectRepository projectRepository;
    private final ProjectGitSyncRunRepository syncRunRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public OrgGitSyncRunsService(
            ProjectRepository projectRepository,
            ProjectGitSyncRunRepository syncRunRepository,
            ProjectAuthorizationService authorizationService,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.syncRunRepository = syncRunRepository;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
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

    @Transactional(readOnly = true)
    public OrgGitSyncRunExport exportRuns(
            UUID organizationId,
            UUID userId,
            String format,
            String source,
            String status,
            UUID projectId
    ) {
        OrgGitSyncRunPageResponse page = listRuns(
                organizationId,
                userId,
                MAX_EXPORT_ROWS,
                0,
                source,
                status,
                projectId
        );
        OrgGitSyncRunExportPayload payload = new OrgGitSyncRunExportPayload(
                organizationId,
                page.totalCount(),
                page.items().size(),
                page.hasMore(),
                page.items()
        );
        String normalizedFormat = normalizeExportFormat(format);
        if ("json".equals(normalizedFormat)) {
            return exportAsJson(payload);
        }
        return exportAsCsv(payload);
    }

    @Transactional(readOnly = true)
    public OrgGitSyncRunFilterCountsResponse getFilterCounts(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);

        List<UUID> projectIds = projectRepository.findByOrganizationIdOrderByUpdatedAtDesc(organizationId)
                .stream()
                .map(ProjectEntity::getId)
                .toList();

        List<OrgGitSyncRunPresetCountResponse> presets = List.of(
                presetCount("failed", projectIds, null, "failed"),
                presetCount("success", projectIds, null, "success"),
                presetCount("manual", projectIds, "manual", null),
                presetCount("scheduled", projectIds, "scheduled", null),
                presetCount("webhook", projectIds, "webhook", null),
                presetCount("failed-manual", projectIds, "manual", "failed"),
                presetCount("failed-scheduled", projectIds, "scheduled", "failed")
        );
        return new OrgGitSyncRunFilterCountsResponse(presets);
    }

    private OrgGitSyncRunPresetCountResponse presetCount(
            String id,
            List<UUID> projectIds,
            String source,
            String status
    ) {
        if (projectIds.isEmpty()) {
            return new OrgGitSyncRunPresetCountResponse(id, 0);
        }
        return new OrgGitSyncRunPresetCountResponse(id, countRuns(projectIds, source, status));
    }

    private long countRuns(List<UUID> projectIds, String source, String status) {
        if (source != null && status != null) {
            return syncRunRepository.countByProjectIdInAndSourceAndStatus(projectIds, source, status);
        }
        if (source != null) {
            return syncRunRepository.countByProjectIdInAndSource(projectIds, source);
        }
        if (status != null) {
            return syncRunRepository.countByProjectIdInAndStatus(projectIds, status);
        }
        return syncRunRepository.countByProjectIdIn(projectIds);
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

    private OrgGitSyncRunExport exportAsJson(OrgGitSyncRunExportPayload payload) {
        try {
            byte[] body = objectMapper.copy()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsBytes(payload);
            return new OrgGitSyncRunExport(
                    body,
                    "application/json; charset=UTF-8",
                    "git-sync-runs-" + payload.organizationId() + ".json"
            );
        } catch (Exception ex) {
            throw new DomainException("INTERNAL_ERROR", "Failed to export git sync runs as JSON");
        }
    }

    private OrgGitSyncRunExport exportAsCsv(OrgGitSyncRunExportPayload payload) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,projectId,projectName,projectKey,gitLinkId,source,status,fileCount,errorMessage,startedAt,finishedAt\n");
        for (OrgGitSyncRunItemResponse item : payload.items()) {
            csv.append(csvCell(item.id()))
                    .append(',').append(csvCell(item.projectId()))
                    .append(',').append(csvCell(item.projectName()))
                    .append(',').append(csvCell(item.projectKey()))
                    .append(',').append(csvCell(item.gitLinkId()))
                    .append(',').append(csvCell(item.source()))
                    .append(',').append(csvCell(item.status()))
                    .append(',').append(item.fileCount())
                    .append(',').append(csvCell(item.errorMessage()))
                    .append(',').append(csvCell(item.startedAt()))
                    .append(',').append(csvCell(item.finishedAt()))
                    .append('\n');
        }
        return new OrgGitSyncRunExport(
                csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "text/csv; charset=UTF-8",
                "git-sync-runs-" + payload.organizationId() + ".csv"
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
}
