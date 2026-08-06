package com.aistudio.api.requirement.dto;

import com.aistudio.domain.common.Priority;
import com.aistudio.domain.requirement.RequirementStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequirementRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 20000) String description,
        Priority priority,
        RequirementStatus status
) {
}
