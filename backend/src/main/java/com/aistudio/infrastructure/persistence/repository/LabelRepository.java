package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.LabelEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<LabelEntity, UUID> {
    List<LabelEntity> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<LabelEntity> findByIdAndProjectId(UUID id, UUID projectId);

    List<LabelEntity> findByProjectIdAndIdIn(UUID projectId, Collection<UUID> ids);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
