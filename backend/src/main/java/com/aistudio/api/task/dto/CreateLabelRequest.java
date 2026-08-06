package com.aistudio.api.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(
        @NotBlank @Size(max = 60) String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color must be a hex like #6B7280")
        String color
) {
}
