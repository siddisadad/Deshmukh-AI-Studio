package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.RequirementEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequirementRepository extends JpaRepository<RequirementEntity, UUID> {
    List<RequirementEntity> findByProjectIdOrderBySortOrderAscCreatedAtAsc(UUID projectId);

    Optional<RequirementEntity> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);

    @Query("""
            SELECT r.projectId AS projectId, COUNT(r) AS count
            FROM RequirementEntity r
            WHERE r.projectId IN :projectIds
            GROUP BY r.projectId
            """)
    List<ProjectCountProjection> countGroupedByProjectId(@Param("projectIds") Collection<UUID> projectIds);
}
