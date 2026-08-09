package com.aistudio.application.codemetadata;

import java.util.List;

public record GitWebhookDelta(List<String> changedPaths, List<String> removedPaths) {

    public boolean hasChanges() {
        return !changedPaths().isEmpty() || !removedPaths().isEmpty();
    }
}
