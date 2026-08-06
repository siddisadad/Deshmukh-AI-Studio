package com.aistudio.api.requirement.dto;

import jakarta.validation.constraints.Size;

public record AiActionRequest(
        @Size(max = 4000) String instructions
) {
}
