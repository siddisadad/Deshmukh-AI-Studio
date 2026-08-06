package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository extends JpaRepository<RequirementEntity, UUID> {
    List<RequirementEntity> findByProjectIdOrderBySortOrderAscCreatedAtAsc(UUID projectId);

    Optional<RequirementEntity> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);
}
