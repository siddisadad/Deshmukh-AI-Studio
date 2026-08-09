package com.aistudio.api.codemetadata.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReplaceCodeMetadataRequest(
        @NotNull @Valid List<CodeFileInput> files
) {
    public record CodeFileInput(
            @NotBlank @Size(max = 500) String path,
            @Size(max = 40) String language,
            @Size(max = 8000) String snippet,
            int sizeBytes
    ) {
    }
}
