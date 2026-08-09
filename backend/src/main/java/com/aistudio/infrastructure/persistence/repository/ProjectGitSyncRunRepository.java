package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ProjectGitSyncRunEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGitSyncRunRepository extends JpaRepository<ProjectGitSyncRunEntity, UUID> {

    List<ProjectGitSyncRunEntity> findByProjectIdOrderByFinishedAtDesc(UUID projectId, Pageable pageable);

    List<ProjectGitSyncRunEntity> findByProjectIdAndSourceOrderByFinishedAtDesc(
            UUID projectId,
            String source,
            Pageable pageable
    );

    List<ProjectGitSyncRunEntity> findByProjectIdAndStatusOrderByFinishedAtDesc(
            UUID projectId,
            String status,
            Pageable pageable
    );

    List<ProjectGitSyncRunEntity> findByProjectIdAndSourceAndStatusOrderByFinishedAtDesc(
            UUID projectId,
            String source,
            String status,
            Pageable pageable
    );
}
