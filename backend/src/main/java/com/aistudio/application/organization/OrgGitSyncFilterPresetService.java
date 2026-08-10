package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.CreateOrgGitSyncFilterPresetRequest;
import com.aistudio.api.organization.dto.OrgGitSyncFilterPresetResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrgGitSyncFilterPresetEntity;
import com.aistudio.infrastructure.persistence.repository.OrgGitSyncFilterPresetRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgGitSyncFilterPresetService {

    private static final int MAX_PRESETS_PER_SCOPE = 12;

    private static final Set<String> ALLOWED_SCOPES = Set.of("overview", "runs");

    private static final Set<String> OVERVIEW_FILTER_KEYS = Set.of(
            "linked", "enabled", "scheduled", "interval", "provider", "status"
    );

    private static final Set<String> RUN_FILTER_KEYS = Set.of("source", "status", "project");

    private static final Map<String, Set<String>> OVERVIEW_ALLOWED_VALUES = Map.of(
            "linked", Set.of("all", "linked", "unlinked"),
            "enabled", Set.of("all", "enabled", "disabled"),
            "scheduled", Set.of("all", "scheduled", "manual"),
            "interval", Set.of("all", "custom", "default"),
            "provider", Set.of("all", "github", "gitlab", "bitbucket"),
            "status", Set.of("all", "success", "failed", "never")
    );

    private static final Map<String, Set<String>> RUN_ALLOWED_VALUES = Map.of(
            "source", Set.of("all", "manual", "scheduled", "webhook"),
            "status", Set.of("all", "success", "failed"),
            "project", Set.of("all")
    );

    private final OrgGitSyncFilterPresetRepository presetRepository;
    private final ProjectAuthorizationService authorizationService;
    private final OrgGitSyncOverviewService overviewService;
    private final OrgGitSyncRunsService runsService;

    public OrgGitSyncFilterPresetService(
            OrgGitSyncFilterPresetRepository presetRepository,
            ProjectAuthorizationService authorizationService,
            OrgGitSyncOverviewService overviewService,
            OrgGitSyncRunsService runsService
    ) {
        this.presetRepository = presetRepository;
        this.authorizationService = authorizationService;
        this.overviewService = overviewService;
        this.runsService = runsService;
    }

    @Transactional(readOnly = true)
    public List<OrgGitSyncFilterPresetResponse> listPresets(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return presetRepository.findByOrganizationIdAndUserIdOrderByCreatedAtAsc(organizationId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrgGitSyncFilterPresetResponse createPreset(
            UUID organizationId,
            UUID userId,
            CreateOrgGitSyncFilterPresetRequest request
    ) {
        authorizationService.requireOrgMember(organizationId, userId);
        String scope = normalizeScope(request.scope());
        String label = normalizeLabel(request.label());
        Map<String, String> filters = normalizeFilters(scope, request.filters());

        if (presetRepository.countByOrganizationIdAndUserIdAndScope(organizationId, userId, scope)
                >= MAX_PRESETS_PER_SCOPE) {
            throw new DomainException(
                    "VALIDATION_ERROR",
                    "Maximum " + MAX_PRESETS_PER_SCOPE + " saved " + scope + " presets");
        }

        List<OrgGitSyncFilterPresetEntity> existing =
                presetRepository.findByOrganizationIdAndUserIdOrderByCreatedAtAsc(organizationId, userId);
        for (OrgGitSyncFilterPresetEntity preset : existing) {
            if (preset.getScope().equals(scope) && filtersEqual(preset.getFilters(), filters)) {
                throw new DomainException("VALIDATION_ERROR", "These filters are already saved");
            }
            if (preset.getScope().equals(scope) && preset.getLabel().equalsIgnoreCase(label)) {
                throw new DomainException("VALIDATION_ERROR", "A preset with this name already exists");
            }
        }

        OrgGitSyncFilterPresetEntity entity = new OrgGitSyncFilterPresetEntity();
        entity.setOrganizationId(organizationId);
        entity.setUserId(userId);
        entity.setScope(scope);
        entity.setLabel(label);
        entity.setFilters(filters);
        presetRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public void deletePreset(UUID organizationId, UUID userId, UUID presetId) {
        authorizationService.requireOrgMember(organizationId, userId);
        OrgGitSyncFilterPresetEntity entity = presetRepository
                .findByIdAndOrganizationIdAndUserId(presetId, organizationId, userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Filter preset not found"));
        presetRepository.delete(entity);
    }

    private OrgGitSyncFilterPresetResponse toResponse(OrgGitSyncFilterPresetEntity entity) {
        long count = entity.getScope().equals("overview")
                ? overviewService.countSavedPresetMatches(entity.getOrganizationId(), entity.getFilters())
                : runsService.countSavedPresetMatches(entity.getOrganizationId(), entity.getFilters());
        return new OrgGitSyncFilterPresetResponse(
                entity.getId(),
                entity.getScope(),
                entity.getLabel(),
                new HashMap<>(entity.getFilters()),
                count,
                entity.getCreatedAt()
        );
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "scope is required");
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCOPES.contains(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "scope must be overview or runs");
        }
        return normalized;
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "Preset name is required");
        }
        String normalized = label.trim();
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40);
        }
        return normalized;
    }

    private Map<String, String> normalizeFilters(String scope, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            throw new DomainException("VALIDATION_ERROR", "filters are required");
        }
        Set<String> allowedKeys = scope.equals("overview") ? OVERVIEW_FILTER_KEYS : RUN_FILTER_KEYS;
        Map<String, Set<String>> allowedValues =
                scope.equals("overview") ? OVERVIEW_ALLOWED_VALUES : RUN_ALLOWED_VALUES;

        Map<String, String> normalized = new HashMap<>();
        for (String key : allowedKeys) {
            String value = filters.get(key);
            if (value == null || value.isBlank()) {
                normalized.put(key, "all");
                continue;
            }
            String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
            Set<String> values = allowedValues.get(key);
            if (key.equals("project") && scope.equals("runs")) {
                if ("all".equals(normalizedValue)) {
                    normalized.put(key, "all");
                } else {
                    normalized.put(key, value.trim());
                }
                continue;
            }
            if (values == null || !values.contains(normalizedValue)) {
                throw new DomainException("VALIDATION_ERROR", "Invalid filter value for " + key);
            }
            normalized.put(key, normalizedValue);
        }
        return normalized;
    }

    private boolean filtersEqual(Map<String, String> left, Map<String, String> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (Map.Entry<String, String> entry : left.entrySet()) {
            String rightValue = right.get(entry.getKey());
            if (rightValue == null || !rightValue.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
