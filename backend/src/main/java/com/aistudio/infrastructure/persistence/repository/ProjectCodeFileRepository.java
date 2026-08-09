package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ProjectCodeFileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCodeFileRepository extends JpaRepository<ProjectCodeFileEntity, UUID> {
    List<ProjectCodeFileEntity> findByProjectIdOrderByPathAsc(UUID projectId);

    void deleteByProjectId(UUID projectId);

    int countByProjectId(UUID projectId);
}
