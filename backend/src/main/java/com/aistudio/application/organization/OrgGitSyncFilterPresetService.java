package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.CreateOrgGitSyncFilterPresetRequest;
import com.aistudio.api.organization.dto.OrgGitSyncFilterPresetResponse;
import com.aistudio.api.organization.dto.UpdateOrgGitSyncFilterPresetRequest;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrgGitSyncFilterPresetEntity;
import com.aistudio.infrastructure.persistence.entity.UserEntity;
import com.aistudio.infrastructure.persistence.repository.OrgGitSyncFilterPresetRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final String VISIBILITY_PRIVATE = "private";
    private static final String VISIBILITY_ORG = "org";

    private static final Set<String> ALLOWED_SCOPES = Set.of("overview", "runs");

    private static final Set<String> ALLOWED_VISIBILITY = Set.of(VISIBILITY_PRIVATE, VISIBILITY_ORG);

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
    private final UserRepository userRepository;

    public OrgGitSyncFilterPresetService(
            OrgGitSyncFilterPresetRepository presetRepository,
            ProjectAuthorizationService authorizationService,
            OrgGitSyncOverviewService overviewService,
            OrgGitSyncRunsService runsService,
            UserRepository userRepository
    ) {
        this.presetRepository = presetRepository;
        this.authorizationService = authorizationService;
        this.overviewService = overviewService;
        this.runsService = runsService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrgGitSyncFilterPresetResponse> listPresets(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);

        List<OrgGitSyncFilterPresetEntity> privatePresets =
                presetRepository.findByOrganizationIdAndUserIdAndVisibilityOrderByCreatedAtAsc(
                        organizationId,
                        userId,
                        VISIBILITY_PRIVATE
                );
        List<OrgGitSyncFilterPresetEntity> orgPresets =
                presetRepository.findByOrganizationIdAndVisibilityOrderByCreatedAtAsc(
                        organizationId,
                        VISIBILITY_ORG
                );

        List<OrgGitSyncFilterPresetEntity> merged = new ArrayList<>(privatePresets.size() + orgPresets.size());
        merged.addAll(privatePresets);
        merged.addAll(orgPresets);

        Set<UUID> userIds = new HashSet<>();
        for (OrgGitSyncFilterPresetEntity preset : merged) {
            userIds.add(preset.getUserId());
        }
        Map<UUID, String> displayNames = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            displayNames.put(user.getId(), user.getDisplayName());
        }

        return merged.stream()
                .map(entity -> toResponse(entity, displayNames.get(entity.getUserId())))
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
        String visibility = normalizeVisibility(request.visibility());
        Map<String, String> filters = normalizeFilters(scope, request.filters());

        if (VISIBILITY_ORG.equals(visibility)) {
            authorizationService.requireOrgOwner(organizationId, userId);
        }

        if (VISIBILITY_ORG.equals(visibility)) {
            if (presetRepository.countByOrganizationIdAndScopeAndVisibility(organizationId, scope, VISIBILITY_ORG)
                    >= MAX_PRESETS_PER_SCOPE) {
                throw new DomainException(
                        "VALIDATION_ERROR",
                        "Maximum " + MAX_PRESETS_PER_SCOPE + " org-shared " + scope + " presets");
            }
        } else {
            if (presetRepository.countByOrganizationIdAndUserIdAndScopeAndVisibility(
                    organizationId,
                    userId,
                    scope,
                    VISIBILITY_PRIVATE
            ) >= MAX_PRESETS_PER_SCOPE) {
                throw new DomainException(
                        "VALIDATION_ERROR",
                        "Maximum " + MAX_PRESETS_PER_SCOPE + " saved " + scope + " presets");
            }
        }

        List<OrgGitSyncFilterPresetEntity> existing = VISIBILITY_ORG.equals(visibility)
                ? presetRepository.findByOrganizationIdAndVisibilityOrderByCreatedAtAsc(organizationId, VISIBILITY_ORG)
                : presetRepository.findByOrganizationIdAndUserIdAndVisibilityOrderByCreatedAtAsc(
                        organizationId,
                        userId,
                        VISIBILITY_PRIVATE
                );

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
        entity.setVisibility(visibility);
        presetRepository.save(entity);

        String displayName = userRepository.findById(userId).map(UserEntity::getDisplayName).orElse(null);
        return toResponse(entity, displayName);
    }

    @Transactional
    public OrgGitSyncFilterPresetResponse updatePreset(
            UUID organizationId,
            UUID userId,
            UUID presetId,
            UpdateOrgGitSyncFilterPresetRequest request
    ) {
        authorizationService.requireOrgMember(organizationId, userId);
        OrgGitSyncFilterPresetEntity entity = requireEditablePreset(organizationId, userId, presetId);

        boolean hasLabel = request.label() != null && !request.label().isBlank();
        boolean hasFilters = request.filters() != null && !request.filters().isEmpty();
        if (!hasLabel && !hasFilters) {
            throw new DomainException("VALIDATION_ERROR", "Provide a new name and/or filters to update");
        }

        List<OrgGitSyncFilterPresetEntity> siblings = VISIBILITY_ORG.equals(entity.getVisibility())
                ? presetRepository.findByOrganizationIdAndVisibilityOrderByCreatedAtAsc(
                        organizationId, VISIBILITY_ORG)
                : presetRepository.findByOrganizationIdAndUserIdAndVisibilityOrderByCreatedAtAsc(
                        organizationId, entity.getUserId(), VISIBILITY_PRIVATE);

        if (hasLabel) {
            String label = normalizeLabel(request.label());
            if (!entity.getLabel().equalsIgnoreCase(label)) {
                for (OrgGitSyncFilterPresetEntity sibling : siblings) {
                    if (sibling.getId().equals(entity.getId())) {
                        continue;
                    }
                    if (sibling.getScope().equals(entity.getScope()) && sibling.getLabel().equalsIgnoreCase(label)) {
                        throw new DomainException("VALIDATION_ERROR", "A preset with this name already exists");
                    }
                }
                entity.setLabel(label);
            }
        }

        if (hasFilters) {
            Map<String, String> filters = normalizeFilters(entity.getScope(), request.filters());
            if (!filtersEqual(entity.getFilters(), filters)) {
                for (OrgGitSyncFilterPresetEntity sibling : siblings) {
                    if (sibling.getId().equals(entity.getId())) {
                        continue;
                    }
                    if (sibling.getScope().equals(entity.getScope()) && filtersEqual(sibling.getFilters(), filters)) {
                        throw new DomainException("VALIDATION_ERROR", "These filters are already saved");
                    }
                }
                entity.setFilters(filters);
            }
        }

        presetRepository.save(entity);

        String displayName = userRepository.findById(entity.getUserId()).map(UserEntity::getDisplayName).orElse(null);
        return toResponse(entity, displayName);
    }

    @Transactional
    public void deletePreset(UUID organizationId, UUID userId, UUID presetId) {
        authorizationService.requireOrgMember(organizationId, userId);
        OrgGitSyncFilterPresetEntity entity = requireEditablePreset(organizationId, userId, presetId);
        presetRepository.delete(entity);
    }

    private OrgGitSyncFilterPresetEntity requireEditablePreset(
            UUID organizationId,
            UUID userId,
            UUID presetId
    ) {
        OrgGitSyncFilterPresetEntity entity = presetRepository
                .findByIdAndOrganizationId(presetId, organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Filter preset not found"));

        if (VISIBILITY_PRIVATE.equals(entity.getVisibility())) {
            if (!entity.getUserId().equals(userId)) {
                throw new DomainException("NOT_FOUND", "Filter preset not found");
            }
            return entity;
        }

        if (entity.getUserId().equals(userId)) {
            return entity;
        }

        MembershipEntity membership = authorizationService.requireOrgMember(organizationId, userId);
        if (membership.getRole() != OrgRole.OWNER && membership.getRole() != OrgRole.ADMIN) {
            throw new DomainException("NOT_FOUND", "Filter preset not found");
        }
        return entity;
    }

    private OrgGitSyncFilterPresetResponse toResponse(OrgGitSyncFilterPresetEntity entity, String createdByDisplayName) {
        long count = entity.getScope().equals("overview")
                ? overviewService.countSavedPresetMatches(entity.getOrganizationId(), entity.getFilters())
                : runsService.countSavedPresetMatches(entity.getOrganizationId(), entity.getFilters());
        return new OrgGitSyncFilterPresetResponse(
                entity.getId(),
                entity.getScope(),
                entity.getLabel(),
                new HashMap<>(entity.getFilters()),
                count,
                entity.getVisibility(),
                entity.getUserId(),
                createdByDisplayName,
                entity.getCreatedAt()
        );
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return VISIBILITY_PRIVATE;
        }
        String normalized = visibility.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_VISIBILITY.contains(normalized)) {
            throw new DomainException("VALIDATION_ERROR", "visibility must be private or org");
        }
        return normalized;
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
