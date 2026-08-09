package com.aistudio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aistudio.git")
public record GitProperties(
        String provider,
        String apiToken,
        String apiBaseUrl,
        String publicApiBaseUrl,
        String gitlabApiToken,
        String gitlabApiBaseUrl,
        String bitbucketApiToken,
        String bitbucketApiBaseUrl,
        Boolean fetchFileContent,
        Integer maxSnippetBytes,
        Integer maxContentFetchBytes,
        Boolean webhookDeltaSync,
        Boolean scheduledSyncEnabled,
        Integer scheduledSyncIntervalMs
) {

    public boolean fetchFileContentEnabled() {
        return fetchFileContent == null || fetchFileContent;
    }

    public boolean webhookDeltaSyncEnabled() {
        return webhookDeltaSync == null || webhookDeltaSync;
    }

    public boolean isScheduledSyncEnabled() {
        return scheduledSyncEnabled == null || scheduledSyncEnabled;
    }

    public long effectiveScheduledSyncIntervalMs() {
        return scheduledSyncIntervalMs == null || scheduledSyncIntervalMs <= 0 ? 3600000L : scheduledSyncIntervalMs;
    }

    public int effectiveMaxSnippetBytes() {
        return maxSnippetBytes == null || maxSnippetBytes <= 0 ? 4000 : maxSnippetBytes;
    }

    public int effectiveMaxContentFetchBytes() {
        return maxContentFetchBytes == null || maxContentFetchBytes <= 0 ? 512000 : maxContentFetchBytes;
    }
}
