package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.project.ProjectStatus;
import com.aistudio.infrastructure.persistence.entity.ProjectEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    boolean existsByOrganizationIdAndProjectKeyIgnoreCase(UUID organizationId, String projectKey);

    List<ProjectEntity> findByOrganizationIdAndStatusOrderByUpdatedAtDesc(UUID organizationId, ProjectStatus status);

    List<ProjectEntity> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

    List<ProjectEntity> findByIdInAndStatusOrderByUpdatedAtDesc(Collection<UUID> ids, ProjectStatus status);

    Optional<ProjectEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
