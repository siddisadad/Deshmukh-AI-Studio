package com.aistudio.api.document.dto;

import com.aistudio.domain.document.DocumentType;
import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @Size(max = 300) String title,
        DocumentType docType,
        @Size(max = 200000) String contentMd
) {
}
