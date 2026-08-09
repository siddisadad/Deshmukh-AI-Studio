package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ProjectGitSyncRunEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGitSyncRunRepository extends JpaRepository<ProjectGitSyncRunEntity, UUID> {

    List<ProjectGitSyncRunEntity> findByProjectIdOrderByFinishedAtDesc(UUID projectId, Pageable pageable);

    List<ProjectGitSyncRunEntity> findByProjectIdInOrderByFinishedAtDesc(Collection<UUID> projectIds, Pageable pageable);

    List<ProjectGitSyncRunEntity> findByProjectIdInAndSourceOrderByFinishedAtDesc(
            Collection<UUID> projectIds,
            String source,
            Pageable pageable
    );

    List<ProjectGitSyncRunEntity> findByProjectIdInAndStatusOrderByFinishedAtDesc(
            Collection<UUID> projectIds,
            String status,
            Pageable pageable
    );

    List<ProjectGitSyncRunEntity> findByProjectIdInAndSourceAndStatusOrderByFinishedAtDesc(
            Collection<UUID> projectIds,
            String source,
            String status,
            Pageable pageable
    );

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

    long countByProjectId(UUID projectId);

    long countByProjectIdIn(Collection<UUID> projectIds);

    long countByProjectIdInAndSource(Collection<UUID> projectIds, String source);

    long countByProjectIdInAndStatus(Collection<UUID> projectIds, String status);

    long countByProjectIdInAndSourceAndStatus(Collection<UUID> projectIds, String source, String status);

    long countByProjectIdAndSource(UUID projectId, String source);

    long countByProjectIdAndStatus(UUID projectId, String status);

    long countByProjectIdAndSourceAndStatus(UUID projectId, String source, String status);
}
