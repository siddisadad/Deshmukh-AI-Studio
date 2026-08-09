package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ProjectGitLinkEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGitLinkRepository extends JpaRepository<ProjectGitLinkEntity, UUID> {
    Optional<ProjectGitLinkEntity> findByProjectId(UUID projectId);

    List<ProjectGitLinkEntity> findByEnabledTrue();

    List<ProjectGitLinkEntity> findByProjectIdIn(Collection<UUID> projectIds);
}
