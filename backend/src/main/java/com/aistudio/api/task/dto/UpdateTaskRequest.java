package com.aistudio.api.task.dto;

import com.aistudio.domain.common.Priority;
import com.aistudio.domain.task.TaskStatus;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateTaskRequest(
        @Size(max = 300) String title,
        @Size(max = 20000) String description,
        Priority priority,
        TaskStatus status,
        UUID requirementId,
        /** When true, clears the requirement link (needed because null requirementId means "omit" on PATCH). */
        Boolean clearRequirementId,
        UUID assigneeId,
        /** When true, clears the assignee (needed because null assigneeId means "omit" on PATCH). */
        Boolean clearAssigneeId,
        List<UUID> labelIds,
        Integer sortOrder
) {
}
