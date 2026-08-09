package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.OrgAiPolicySimulationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgAiPolicySimulationRepository extends JpaRepository<OrgAiPolicySimulationEntity, UUID> {

    List<OrgAiPolicySimulationEntity> findByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId,
            Pageable pageable
    );

    Optional<OrgAiPolicySimulationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
