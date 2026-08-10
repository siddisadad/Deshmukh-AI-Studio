package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrgGitSyncFilterPresetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgGitSyncFilterPresetRepository extends JpaRepository<OrgGitSyncFilterPresetEntity, UUID> {

    List<OrgGitSyncFilterPresetEntity> findByOrganizationIdAndUserIdOrderByCreatedAtAsc(
            UUID organizationId,
            UUID userId
    );

    List<OrgGitSyncFilterPresetEntity> findByOrganizationIdAndUserIdAndVisibilityOrderByCreatedAtAsc(
            UUID organizationId,
            UUID userId,
            String visibility
    );

    List<OrgGitSyncFilterPresetEntity> findByOrganizationIdAndVisibilityOrderByCreatedAtAsc(
            UUID organizationId,
            String visibility
    );

    long countByOrganizationIdAndUserIdAndScope(UUID organizationId, UUID userId, String scope);

    long countByOrganizationIdAndUserIdAndScopeAndVisibility(
            UUID organizationId,
            UUID userId,
            String scope,
            String visibility
    );

    long countByOrganizationIdAndScopeAndVisibility(UUID organizationId, String scope, String visibility);

    Optional<OrgGitSyncFilterPresetEntity> findByIdAndOrganizationIdAndUserId(
            UUID id,
            UUID organizationId,
            UUID userId
    );

    Optional<OrgGitSyncFilterPresetEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
