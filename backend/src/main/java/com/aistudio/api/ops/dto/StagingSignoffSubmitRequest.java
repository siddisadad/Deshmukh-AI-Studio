package com.aistudio.api.ops.dto;

import jakarta.validation.constraints.NotBlank;

public record StagingSignoffSubmitRequest(
        @NotBlank String reportJson,
        String s3Uri
) {
}
