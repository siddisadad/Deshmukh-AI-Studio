package com.aistudio.application.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.billing.dunning-enabled", havingValue = "true")
public class BillingDunningScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingDunningScheduler.class);

    private final BillingDunningService dunningService;

    public BillingDunningScheduler(BillingDunningService dunningService) {
        this.dunningService = dunningService;
    }

    @Scheduled(fixedDelayString = "${aistudio.billing.dunning-scheduler-interval-ms:259200000}")
    public void runScheduledDunning() {
        BillingDunningService.BillingDunningRunResult result = dunningService.runScheduledDunning();
        if (result.notified() > 0) {
            log.info(
                    "Billing dunning run: processed={} notified={} skipped={}",
                    result.processed(),
                    result.notified(),
                    result.skipped());
        }
    }
}
