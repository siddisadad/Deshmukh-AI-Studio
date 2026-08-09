package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrgGitCredentialEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgGitCredentialRepository extends JpaRepository<OrgGitCredentialEntity, UUID> {

    List<OrgGitCredentialEntity> findByOrganizationIdOrderByProviderAsc(UUID organizationId);

    Optional<OrgGitCredentialEntity> findByOrganizationIdAndProvider(UUID organizationId, String provider);

    void deleteByOrganizationIdAndProvider(UUID organizationId, String provider);
}
