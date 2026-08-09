package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.ai.OrgAiPolicyChangeStatus;
import com.aistudio.infrastructure.persistence.entity.OrgAiPolicyChangeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgAiPolicyChangeRepository extends JpaRepository<OrgAiPolicyChangeEntity, UUID> {

    Optional<OrgAiPolicyChangeEntity> findByOrganizationIdAndStatus(UUID organizationId, OrgAiPolicyChangeStatus status);

    List<OrgAiPolicyChangeEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, org.springframework.data.domain.Pageable pageable);
}
