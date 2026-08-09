package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrganizationPluginPackEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationPluginPackEntity.Pk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationPluginPackRepository extends JpaRepository<OrganizationPluginPackEntity, Pk> {

    List<OrganizationPluginPackEntity> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationIdAndPackId(UUID organizationId, String packId);
}
