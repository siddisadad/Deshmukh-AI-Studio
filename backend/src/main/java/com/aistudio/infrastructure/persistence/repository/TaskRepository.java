package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.task.TaskStatus;
import com.aistudio.infrastructure.persistence.entity.TaskEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    @EntityGraph(attributePaths = "labels")
    List<TaskEntity> findByProjectIdOrderBySortOrderAscCreatedAtAsc(UUID projectId);

    @EntityGraph(attributePaths = "labels")
    Optional<TaskEntity> findWithLabelsById(UUID id);

    long countByProjectIdAndStatusNot(UUID projectId, TaskStatus status);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);
}
