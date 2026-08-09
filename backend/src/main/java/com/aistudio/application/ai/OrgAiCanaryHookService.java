package com.aistudio.application.ai;

import com.aistudio.domain.ai.OrgAiCanaryHookAction;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.OrgAiCanaryOutcomeEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgAiCanaryHookService {

    public record Evaluation(OrgAiCanaryHookAction action, String reason, OrgAiCanaryOutcomeEntity metrics) {
    }

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrgAiCanaryOutcomeService outcomeService;
    private final OrgAiCanaryHookNotifier notifier;
    private final com.aistudio.application.billing.BillingService billingService;

    public OrgAiCanaryHookService(
            OrganizationSubscriptionRepository subscriptionRepository,
            OrgAiCanaryOutcomeService outcomeService,
            OrgAiCanaryHookNotifier notifier,
            com.aistudio.application.billing.BillingService billingService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.outcomeService = outcomeService;
        this.notifier = notifier;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public List<UUID> organizationsWithActiveHooks() {
        return subscriptionRepository.findCanaryHookCandidates().stream()
                .map(OrganizationSubscriptionEntity::getOrganizationId)
                .toList();
    }

    @Transactional
    public Evaluation evaluateAndAct(UUID organizationId) {
        OrganizationSubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);
        if (sub == null || !hasActiveCanary(sub)) {
            return new Evaluation(
                    OrgAiCanaryHookAction.NONE,
                    "No active canary rollout",
                    outcomeService.metrics(organizationId));
        }
        OrgAiCanaryOutcomeEntity metrics = outcomeService.metrics(organizationId);
        Evaluation decision = decide(sub, metrics);
        if (decision.action() == OrgAiCanaryHookAction.PROMOTED) {
            billingService.promoteCanary(organizationId);
            outcomeService.reset(organizationId);
            notifier.notifyIfConfigured(
                    sub.getAiCanaryHookWebhookUrl(),
                    organizationId,
                    OrgAiCanaryHookAction.PROMOTED,
                    decision.reason(),
                    metrics.getCanarySuccessCount(),
                    metrics.getCanaryFailureCount(),
                    metrics.getStableSuccessCount(),
                    metrics.getStableFailureCount());
            return new Evaluation(
                    OrgAiCanaryHookAction.PROMOTED,
                    decision.reason(),
                    outcomeService.metrics(organizationId));
        }
        if (decision.action() == OrgAiCanaryHookAction.ABORTED) {
            billingService.abortCanary(organizationId);
            outcomeService.reset(organizationId);
            notifier.notifyIfConfigured(
                    sub.getAiCanaryHookWebhookUrl(),
                    organizationId,
                    OrgAiCanaryHookAction.ABORTED,
                    decision.reason(),
                    metrics.getCanarySuccessCount(),
                    metrics.getCanaryFailureCount(),
                    metrics.getStableSuccessCount(),
                    metrics.getStableFailureCount());
            return new Evaluation(
                    OrgAiCanaryHookAction.ABORTED,
                    decision.reason(),
                    outcomeService.metrics(organizationId));
        }
        return decision;
    }

    private Evaluation decide(OrganizationSubscriptionEntity sub, OrgAiCanaryOutcomeEntity metrics) {
        long canarySuccess = metrics.getCanarySuccessCount();
        long canaryFailure = metrics.getCanaryFailureCount();
        long canaryTotal = canarySuccess + canaryFailure;
        double canaryErrorRate = errorRatePercent(canarySuccess, canaryFailure);

        if (sub.isAiCanaryAutoAbortEnabled()
                && canaryTotal >= sub.getAiCanaryMinSamples()
                && canaryErrorRate >= sub.getAiCanaryAbortErrorRatePercent()) {
            return new Evaluation(
                    OrgAiCanaryHookAction.ABORTED,
                    "Canary error rate "
                            + formatRate(canaryErrorRate)
                            + "% exceeds abort threshold "
                            + sub.getAiCanaryAbortErrorRatePercent()
                            + "%",
                    metrics);
        }
        if (sub.isAiCanaryAutoPromoteEnabled()
                && canaryTotal >= sub.getAiCanaryPromoteMinSamples()
                && canaryErrorRate <= sub.getAiCanaryPromoteMaxErrorRatePercent()) {
            return new Evaluation(
                    OrgAiCanaryHookAction.PROMOTED,
                    "Canary error rate "
                            + formatRate(canaryErrorRate)
                            + "% within promote threshold "
                            + sub.getAiCanaryPromoteMaxErrorRatePercent()
                            + "%",
                    metrics);
        }
        return new Evaluation(
                OrgAiCanaryHookAction.NONE,
                "Canary metrics below automation thresholds",
                metrics);
    }

    private static boolean hasActiveCanary(OrganizationSubscriptionEntity sub) {
        return sub.getAiCanaryProviderChain() != null
                && !sub.getAiCanaryProviderChain().isBlank()
                && sub.getAiCanaryPercent() != null
                && sub.getAiCanaryPercent() > 0;
    }

    private static double errorRatePercent(long success, long failure) {
        long total = success + failure;
        if (total == 0) {
            return 0.0;
        }
        return (failure * 100.0) / total;
    }

    private static String formatRate(double rate) {
        return String.format("%.1f", rate);
    }
}
