package com.aistudio.api.context.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertContextAssetRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200000) String content,
        String metadata
) {
}
