package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ProjectGitSyncRunEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGitSyncRunRepository extends JpaRepository<ProjectGitSyncRunEntity, UUID> {

    List<ProjectGitSyncRunEntity> findByProjectIdOrderByFinishedAtDesc(UUID projectId, Pageable pageable);
}
