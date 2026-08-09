package com.aistudio.api.export.dto;

import java.util.List;

public record SiemExportRunResponse(
        int processed,
        int exported,
        int failed,
        List<String> messages
) {
}
