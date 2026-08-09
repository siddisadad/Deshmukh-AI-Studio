package com.aistudio.api.codemetadata.dto;

import java.util.List;

public record GitConnectionTestResponse(
        boolean ok,
        String message,
        List<GitConnectionCheckResponse> checks
) {
}
