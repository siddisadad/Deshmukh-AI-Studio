package com.aistudio.api.task.dto;

import com.aistudio.domain.common.Priority;
import com.aistudio.domain.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 20000) String description,
        Priority priority,
        TaskStatus status,
        UUID requirementId,
        UUID assigneeId,
        List<UUID> labelIds
) {
}
