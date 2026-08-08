package com.aistudio.api.ai.dto;

public record RetentionPurgeRequest(Boolean complianceExport) {

    public boolean complianceExportRequested() {
        return complianceExport != null && complianceExport;
    }
}
