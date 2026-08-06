package com.aistudio.api.document.dto;

import jakarta.validation.constraints.Size;

public record GenerateDocumentRequest(
        @Size(max = 4000) String instructions
) {
}
