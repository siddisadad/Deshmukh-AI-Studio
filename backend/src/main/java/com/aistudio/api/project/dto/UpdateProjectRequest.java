package com.aistudio.api.project.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 200) String name,
        @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "projectKey must be 2-10 chars, uppercase alphanumeric, starting with a letter")
        String projectKey,
        @Size(max = 5000) String description
) {
}
