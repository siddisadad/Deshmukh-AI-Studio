package com.aistudio.application.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.ai.canary-hook-eval-enabled", havingValue = "true")
public class OrgAiCanaryHookScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrgAiCanaryHookScheduler.class);

    private final OrgAiCanaryHookService hookService;

    public OrgAiCanaryHookScheduler(OrgAiCanaryHookService hookService) {
        this.hookService = hookService;
    }

    @Scheduled(fixedDelayString = "${aistudio.ai.canary-hook-eval-interval-ms:300000}")
    public void evaluateActiveCanaryRollouts() {
        for (java.util.UUID organizationId : hookService.organizationsWithActiveHooks()) {
            try {
                OrgAiCanaryHookService.Evaluation result = hookService.evaluateAndAct(organizationId);
                if (result.action() != com.aistudio.domain.ai.OrgAiCanaryHookAction.NONE) {
                    log.info(
                            "Canary hook {} for org {}: {}",
                            result.action(),
                            organizationId,
                            result.reason());
                }
            } catch (Exception ex) {
                log.warn("Canary hook evaluation failed for org {}: {}", organizationId, ex.getMessage());
            }
        }
    }
}
