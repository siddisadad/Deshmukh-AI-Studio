package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrgGitCredentialEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgGitCredentialEventRepository extends JpaRepository<OrgGitCredentialEventEntity, UUID> {

    List<OrgGitCredentialEventEntity> findByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId,
            Pageable pageable
    );
}
