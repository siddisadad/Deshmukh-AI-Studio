package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgSsoIdpRepository extends JpaRepository<OrgSsoIdpEntity, UUID> {

    List<OrgSsoIdpEntity> findByOrganizationIdOrderByDisplayNameAsc(UUID organizationId);

    List<OrgSsoIdpEntity> findByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(UUID organizationId);

    List<OrgSsoIdpEntity> findByEnabledTrueOrderByDisplayNameAsc();

    Optional<OrgSsoIdpEntity> findByOrganizationIdAndSlug(UUID organizationId, String slug);

    Optional<OrgSsoIdpEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
