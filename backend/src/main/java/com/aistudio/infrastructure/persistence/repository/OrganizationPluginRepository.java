package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrganizationPluginEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationPluginRepository extends JpaRepository<OrganizationPluginEntity, OrganizationPluginEntity.Pk> {

    List<OrganizationPluginEntity> findByOrganizationId(UUID organizationId);

    Optional<OrganizationPluginEntity> findByOrganizationIdAndPluginId(UUID organizationId, String pluginId);
}
