package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.domain.task.TaskStatus;
import java.util.UUID;

public interface ProjectStatusCountProjection {
    UUID getProjectId();

    TaskStatus getStatus();

    long getCount();
}
