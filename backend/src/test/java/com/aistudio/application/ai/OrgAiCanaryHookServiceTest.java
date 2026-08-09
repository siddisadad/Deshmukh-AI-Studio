package com.aistudio.application.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aistudio.domain.ai.OrgAiCanaryHookAction;
import com.aistudio.infrastructure.persistence.entity.OrgAiCanaryOutcomeEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgAiCanaryHookServiceTest {

    @Mock OrganizationSubscriptionRepository subscriptionRepository;
    @Mock OrgAiCanaryOutcomeService outcomeService;
    @Mock OrgAiCanaryHookNotifier notifier;
    @Mock com.aistudio.application.billing.BillingService billingService;

    OrgAiCanaryHookService service;
    UUID orgId;

    @BeforeEach
    void setUp() {
        service = new OrgAiCanaryHookService(
                subscriptionRepository, outcomeService, notifier, billingService);
        orgId = UUID.randomUUID();
    }

    @Test
    void autoAbortsWhenErrorRateExceedsThreshold() {
        OrganizationSubscriptionEntity sub = activeCanarySubscription();
        sub.setAiCanaryAutoAbortEnabled(true);
        sub.setAiCanaryMinSamples(10);
        sub.setAiCanaryAbortErrorRatePercent(40);
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));

        OrgAiCanaryOutcomeEntity metrics = metrics(4, 6, 0, 0);
        OrgAiCanaryOutcomeEntity cleared = emptyMetrics();
        when(outcomeService.metrics(orgId)).thenReturn(metrics, cleared);

        OrgAiCanaryHookService.Evaluation result = service.evaluateAndAct(orgId);

        assertThat(result.action()).isEqualTo(OrgAiCanaryHookAction.ABORTED);
        verify(billingService).abortCanary(orgId);
        verify(outcomeService).reset(orgId);
    }

    @Test
    void autoPromotesWhenErrorRateWithinThreshold() {
        OrganizationSubscriptionEntity sub = activeCanarySubscription();
        sub.setAiCanaryAutoPromoteEnabled(true);
        sub.setAiCanaryPromoteMinSamples(20);
        sub.setAiCanaryPromoteMaxErrorRatePercent(10);
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));

        OrgAiCanaryOutcomeEntity metrics = metrics(18, 2, 0, 0);
        when(outcomeService.metrics(orgId)).thenReturn(metrics, emptyMetrics());

        OrgAiCanaryHookService.Evaluation result = service.evaluateAndAct(orgId);

        assertThat(result.action()).isEqualTo(OrgAiCanaryHookAction.PROMOTED);
        verify(billingService).promoteCanary(orgId);
        verify(outcomeService).reset(orgId);
    }

    @Test
    void noActionWhenBelowSampleThreshold() {
        OrganizationSubscriptionEntity sub = activeCanarySubscription();
        sub.setAiCanaryAutoAbortEnabled(true);
        sub.setAiCanaryMinSamples(20);
        when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(sub));

        OrgAiCanaryOutcomeEntity metrics = metrics(1, 9, 0, 0);
        when(outcomeService.metrics(orgId)).thenReturn(metrics);

        OrgAiCanaryHookService.Evaluation result = service.evaluateAndAct(orgId);

        assertThat(result.action()).isEqualTo(OrgAiCanaryHookAction.NONE);
        verify(billingService, never()).abortCanary(any());
        verify(billingService, never()).promoteCanary(any());
    }

  private OrganizationSubscriptionEntity activeCanarySubscription() {
        OrganizationSubscriptionEntity sub = new OrganizationSubscriptionEntity();
        sub.setOrganizationId(orgId);
        sub.setAiCanaryProviderChain("mock");
        sub.setAiCanaryPercent(25);
        return sub;
    }

    private static OrgAiCanaryOutcomeEntity metrics(
            long canarySuccess,
            long canaryFailure,
            long stableSuccess,
            long stableFailure
    ) {
        OrgAiCanaryOutcomeEntity entity = new OrgAiCanaryOutcomeEntity();
        entity.setCanarySuccessCount(canarySuccess);
        entity.setCanaryFailureCount(canaryFailure);
        entity.setStableSuccessCount(stableSuccess);
        entity.setStableFailureCount(stableFailure);
        return entity;
    }

    private OrgAiCanaryOutcomeEntity emptyMetrics() {
        OrgAiCanaryOutcomeEntity entity = new OrgAiCanaryOutcomeEntity();
        entity.setOrganizationId(orgId);
        return entity;
    }
}
