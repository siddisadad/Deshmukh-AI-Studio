package com.aistudio.api.task.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderTasksRequest(
        @NotEmpty List<@Valid TaskOrderUpdate> updates
) {
    public record TaskOrderUpdate(
            @NotNull UUID taskId,
            @NotNull String status,
            @NotNull Integer sortOrder
    ) {
    }
}
