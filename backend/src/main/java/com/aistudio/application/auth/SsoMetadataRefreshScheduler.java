package com.aistudio.application.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.sso.metadata-refresh-enabled", havingValue = "true")
public class SsoMetadataRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(SsoMetadataRefreshScheduler.class);

    private final SsoMetadataRefreshService metadataRefreshService;

    public SsoMetadataRefreshScheduler(SsoMetadataRefreshService metadataRefreshService) {
        this.metadataRefreshService = metadataRefreshService;
    }

    @Scheduled(fixedDelayString = "${aistudio.sso.metadata-refresh-interval-ms:3600000}")
    public void refreshConfiguredIdpMetadata() {
        int refreshed = metadataRefreshService.refreshAllEnabled();
        if (refreshed > 0) {
            log.info("SSO metadata refresh completed for {} IdP(s)", refreshed);
        }
    }
}
