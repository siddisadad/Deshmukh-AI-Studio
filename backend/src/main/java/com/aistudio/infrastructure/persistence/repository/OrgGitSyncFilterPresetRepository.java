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

    long countByOrganizationIdAndUserIdAndScope(UUID organizationId, UUID userId, String scope);

    Optional<OrgGitSyncFilterPresetEntity> findByIdAndOrganizationIdAndUserId(
            UUID id,
            UUID organizationId,
            UUID userId
    );
}
