package com.aistudio.api.requirement.dto;

import com.aistudio.domain.common.Priority;
import com.aistudio.domain.requirement.RequirementStatus;
import jakarta.validation.constraints.Size;

public record UpdateRequirementRequest(
        @Size(max = 300) String title,
        @Size(max = 20000) String description,
        @Size(max = 20000) String improvedDescription,
        @Size(max = 20000) String userStories,
        @Size(max = 20000) String acceptanceCriteria,
        Priority priority,
        RequirementStatus status
) {
}
