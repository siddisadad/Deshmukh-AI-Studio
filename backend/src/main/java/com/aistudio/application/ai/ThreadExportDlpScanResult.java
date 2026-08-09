package com.aistudio.application.ai;

import java.util.List;

public record ThreadExportDlpScanResult(List<ThreadExportDlpMatch> matches) {

    public ThreadExportDlpScanResult {
        matches = List.copyOf(matches);
    }

    public boolean hasMatches() {
        return !matches.isEmpty();
    }
}
