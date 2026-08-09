package com.aistudio.application.codemetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.git.scheduled-sync-enabled", havingValue = "true", matchIfMissing = true)
public class ProjectGitScheduledSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProjectGitScheduledSyncScheduler.class);

    private final ProjectGitSyncService gitSyncService;

    public ProjectGitScheduledSyncScheduler(ProjectGitSyncService gitSyncService) {
        this.gitSyncService = gitSyncService;
    }

    @Scheduled(fixedDelayString = "${aistudio.git.scheduled-sync-interval-ms:3600000}")
    public void enqueueScheduledSyncs() {
        int enqueued = gitSyncService.enqueueScheduledSyncsForEnabledLinks();
        if (enqueued > 0) {
            log.info("Scheduled git sync: enqueued {} CODE_METADATA_SYNC job(s)", enqueued);
        }
    }
}
