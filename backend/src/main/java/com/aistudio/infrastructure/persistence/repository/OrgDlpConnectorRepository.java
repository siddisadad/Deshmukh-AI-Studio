package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.export.OrgDlpConnectorType;
import com.aistudio.infrastructure.persistence.entity.OrgDlpConnectorEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgDlpConnectorRepository extends JpaRepository<OrgDlpConnectorEntity, UUID> {

    List<OrgDlpConnectorEntity> findByOrganizationIdOrderByDisplayNameAsc(UUID organizationId);

    List<OrgDlpConnectorEntity> findByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(UUID organizationId);

    List<OrgDlpConnectorEntity> findByConnectorTypeAndEnabledTrue(OrgDlpConnectorType connectorType);

    Optional<OrgDlpConnectorEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<OrgDlpConnectorEntity> findByOrganizationIdAndSlug(UUID organizationId, String slug);
}
