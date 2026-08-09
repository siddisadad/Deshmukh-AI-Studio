package com.aistudio.api.ops.dto;

import java.util.UUID;

public record StagingSignoffSubmitResponse(
        UUID id,
        String overall,
        String imageTag
) {
}
