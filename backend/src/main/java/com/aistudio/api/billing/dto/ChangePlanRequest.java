package com.aistudio.api.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePlanRequest(
        @NotBlank String planCode
) {
}
